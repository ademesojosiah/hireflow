# HireFlow v3.0 - Technical Decisions and Product Direction

This document records the technical decisions, current solutions, and active plans for HireFlow. It should stay practical: every product problem should map to an implemented or planned technical response.

---

## 1. Platform Technical Decisions

### 1.1 Temporal Fields: `Instant` over `LocalDateTime`

**Decision:** Use `java.time.Instant` for all audit timestamps such as `createdAt` and `updatedAt`.

**Current solution:**

```java
@CreationTimestamp
private Instant createdAt;

@UpdateTimestamp
private Instant updatedAt;
```

**Reason:** `LocalDateTime` is timezone-naive and ambiguous in distributed systems. `Instant` is UTC-anchored, globally unambiguous, and safer for sorting, comparison, audit trails, and integrations across timezones.

---

### 1.2 Async Work Runs Outside Transaction Boundaries

**Decision:** `@Async` methods must be called after transactional work completes. Do not run async work inside an active `@Transactional` method.

**Current solution:**

```java
@Transactional
public User updateUser(User user) {
    return userRepository.save(user);
}

@Async
public void sendWelcomeAsync(User user) {
    emailService.send(user.getEmail(), ...);
}
```

**Reason:** Transactions hold database sessions. Async threads should not inherit or depend on the original transaction context because that can cause lazy-loading failures, closed connection errors, and deadlocks.

---

### 1.3 File Uploads Use Direct Cloudinary Uploads

**Decision:** The backend does not receive resume file bytes. It generates a short-lived Cloudinary upload signature, and the frontend uploads directly to Cloudinary.

**Current solution:**

```text
1. Frontend -> GET /api/v1/uploads/pdf-signature -> Backend
2. Backend signs {timestamp, folder} with API secret
3. Backend returns signature params
4. Frontend -> POST Cloudinary raw upload endpoint
5. Cloudinary returns {secure_url, public_id}
6. Frontend -> POST resume metadata to HireFlow backend
```

**Reason:** Uploading files through the backend wastes bandwidth and memory, blocks application threads, and creates a scaling bottleneck. Cloudinary already provides resilient upload infrastructure.

**Security invariant:** The Cloudinary API secret never leaves the backend. The frontend only receives a signed, time-bound token for a fixed upload folder.

---

## 2. Product Problem Areas

HireFlow is solving the recruitment anxiety gap: candidates lack visibility, hiring managers lack consistent evidence, and both sides make decisions with incomplete information.

Each problem below is tracked with the current plan and technical direction.

---

## 3. Hiring Manager Recommendation

### Problem

HR managers need a clear best-candidate recommendation, not just a list of applicants. Ranking should explain why a candidate is recommended and what risks remain.

### Current Plan

- Store resume profile data in structured fields.
- Use AI resume analysis to extract skills, experience, education, and match indicators.
- Surface matched and unmatched skills against the job requirements.
- Preserve stage transitions through an append-only `StageUpdate` audit trail.
- Allow HR managers to compare top candidates side by side.

- Add a hiring manager recommendation score that combines:
  - role-specific assessment score
  - interview scorecard
  - project consistency score
- Explain every recommendation with evidence, not just a numeric rank.
- Provide recommendation labels such as `Strong Match`, `Needs Review`, and `High Risk`.

### Technical Direction

- Introduce a dedicated recommendation model or service layer instead of mixing ranking logic into controllers.
- Keep all recommendation inputs auditable.
- Store AI explanations separately from raw scores so the UI can show both concise and detailed views.

---

## 4. Technical Questions From Job Posters

### Problem

Job posters need to define role-specific technical questions so candidates are assessed against the actual work, not generic interview prompts.

### Current Plan

- Job descriptions and requirements provide the base criteria for resume matching.
- Interview scorecards provide structured evaluation after interviews.
- Admins and hiring managers can add role-specific technical questions while creating or updating a job listing.
- `JobListing` owns a simple `JobQuestion` child collection.
- Each question stores:
  - `question` - the candidate-facing prompt
  - `answer` - an internal answer guide for HR or future AI grading
- Job listing responses expose the question text, but do not expose the answer guide.
- Updating a job listing replaces the current question list.
- add timer to questions

### Technical Direction

- Applicant answer capture is implemented: when an application is submitted, each question and the applicant's answer are stored as a `questionSnapshot` — an immutable copy of the question text at submission time. This decouples answers from any future edits to the original `JobQuestion`.
- **Q&A is reserved for human reviewers and is intentionally NOT sent to the AI screening service.** The `ApplicationSubmittedEvent` published to Kafka excludes the answers payload entirely. The full Q&A is persisted in `application_answers`, returned in its own section of `GET /api/v1/applications/{id}`, and shown to admins/HR for manual review during the SCREENING stage.
- Add AI-suggested questions from job descriptions only after the poster approves them.
- Add optional scoring rubrics per question when simple answer guides are not enough.
- Add assessment integrity signals such as paste detection, typing time, and focus changes.
- Revisit question versioning only when applicants begin answering questions asynchronously over time.

---

## 5. Disallow Copy and Paste by Applicants

### Problem

Applicants can paste AI-generated or copied answers into assessments, reducing signal quality.

### Current Plan

- No strict anti-paste enforcement is currently documented.
- Resume upload and profile submission are normal form-based workflows.

- Disable paste events in assessment answer fields.
- Track suspicious browser behavior during assessments, such as repeated focus loss or instant large text insertion.
- Add minimum typing-time heuristics for long-form answers.
- Show applicants a clear integrity notice before the assessment starts.
- Avoid relying only on frontend restrictions because browser controls can be bypassed.

### Technical Direction

- Implement anti-paste controls in the frontend assessment UI.
- Send assessment integrity events to the backend.
- Store integrity events as audit metadata, not as automatic rejection triggers.
- Combine anti-paste signals with AI inconsistency scoring before flagging an applicant.

---

## 6. AI Resume Analysis Resmue and job analysis

### Problem

Hiring teams need fast resume screening, but candidates should not be rejected by an unexplained black box.

### Current Plan

- Resume analysis extracts structured profile information.
- AI screening should surface:
  - matched skills
  - unmatched skills
  - match percentage
  - short explanation
- The AI screening result now separates stage outputs:
  - resume/job analysis score, explanation, and HR review note
  - project consistency score, explanation, and HR review note
  - inconsistency risk score, severity, explanation, review note, and recommended human-review action
- The combined `matchPercentage` no longer triggers automatic stage advancement. Instead, the AI screening service compares it against the job listing's `autoPassThreshold` / `autoRejectThreshold` and writes a `ScreeningRecommendation` flag (`AUTO_PASS` / `MANUAL_REVIEW` / `AUTO_REJECT` / `PENDING`) onto `AiScreeningResult`. The application stays in `SCREENING` until an admin/HR manually advances it.

- Add stronger parsing for projects, certifications, tools, seniority, and role-specific experience.
- Compare resume claims directly against job requirements.
- Return confidence levels per extracted field.
- Flag missing or weak evidence without automatically rejecting the applicant.

### Technical Direction

- AI screening runs as 4 independent Kafka events per application: `resume-screening`, `project-consistency`, `inconsistency-analysis`, and `match-summary`. Each event updates only its own fields on `AiScreeningResult` via null-safe partial-merge setters.
- OpenAI is always enabled in the ai-Screening service. Each AI screener has two beans implementing the same interface — the deterministic `Basic*` and the `OpenAi*` variant carrying `@Primary`. If an OpenAI call fails at runtime, the `OpenAi*` screener catches the exception and delegates to the injected `Basic*` fallback. No feature flag gates this; the fallback path is the resilience boundary.
- Keep raw resume metadata separate from normalized profile fields.
- Store AI output as explainable structured data.
- Add validation around AI-generated fields before using them in ranking.
- Keep staged scores separate so recommendation logic can combine them later without losing the original evidence.

---

## 7. Project Consistency

### Problem

Applicants may list projects that do not align with their claimed skills, seniority, or role experience.

### Current Plan

- Project data can be collected from resumes and applicant profiles.
- Resume analysis can identify skills and project descriptions.

- Score whether listed projects support the applicant's claimed skills.
- Detect mismatches such as:
  - senior-level skill claims with only beginner projects
  - project descriptions that do not mention the claimed stack
  - repeated vague project wording
  - projects unrelated to the target role
- Current basic implementation uses **only the resume summary** as evidence. Applicant Q&A answers are explicitly excluded from the project consistency screener — Q&A is reserved for human review.

### Technical Direction

- Add a project consistency scoring component to the recommendation service.
- Store evidence links between skills and project descriptions.
- Use consistency scores as one input into HR recommendations, not as a standalone rejection rule.
- Do not auto-reject from project consistency alone; keep it as an explainable review input.

---

### 4. Microservices Database Ownership: Centralised in Application Service âœ“

Only the Application Service owns a database. The AI Screening Service and Notification Service are stateless processing workers with no database of their own.

**Why:** Giving each microservice its own DB is the textbook advice, but it creates overhead that is not justified until a service's data access patterns genuinely diverge. At this stage, `ai_screening_results` and `notification_attempts` are naturally queried alongside `applications` â€” keeping them in one DB avoids distributed joins, dual writes, and cross-service transactions.

**Service split:**

| Service | DB | Role |
|---|---|---|
| Application Service | YES — owns all tables | Persistence authority; emits domain events |
| AI Screening Service | NO | Consumes per-stage screening topics; runs AI; publishes results back |
| Notification Service | NO | Consumes stage-change and invite events; sends email + SSE; stateless |

**Current Kafka event topics:**

| Topic | Direction | Description |
|---|---|---|
| `application-stage-updated` | hireflow → notification | Triggers stage-change email + SSE to applicant |
| `resume-screening` | hireflow → ai-screening | Per-stage: resume/job analysis |
| `project-consistency` | hireflow → ai-screening | Per-stage: project consistency analysis |
| `inconsistency-analysis` | hireflow → ai-screening | Per-stage: cross-source inconsistency detection |
| `match-summary` | hireflow → ai-screening | Per-stage: combined match percentage and summary |
| `screening-completed` | ai-screening → hireflow | Final screening result merged back into AiScreeningResult |
| `hmanager-invite-email` | hireflow → notification | HMANAGER invite link email delivery |

**Reliability without a DB:** Retry durability for the stateless services is provided by Kafka — retry topics with exponential backoff, dead-letter topics (DLTs) for poison messages. If AI screening fails, the AI service publishes `ScreeningFailed` and the Application Service marks the result as failed. No local DB required.

**When to revisit:** Add a database to a stateless service only if its data access patterns require independent querying at scale, or compliance mandates a separate audit store with long-term retention.

---

---

## 8. Invite-Based HMANAGER Onboarding

### Problem

HR managers cannot self-register. They must be onboarded by an admin who controls which email addresses receive access and which company they belong to.

### Current Plan

- `HMANAGER` is blocked from registering through `POST /api/v1/auth/register`. Attempting it returns a 400 error.
- `ADMIN` can still self-register through the same endpoint.
- Admin sends an invite via `POST /api/v1/admin/invite-manager` with the HR manager's email and an optional `companyId`.
- The backend generates a UUID token, stores it in `hmanager_invitations` with status `PENDING`, and publishes a `HMANAGER_INVITE` Kafka event.
- The Notification Service delivers a branded email to the invited address containing a link: `{frontendBaseUrl}/accept-invite?token={token}`.
- The HR manager opens the link, fills in their name and password, and submits to `POST /api/v1/auth/accept-invite`.
- The backend validates the token, checks the invite is still `PENDING`, creates a verified `HMANAGER` user (optionally linked to the company), marks the invite `ACCEPTED`, and returns a JWT `AuthResponse` — the user is logged in immediately with no extra OTP step needed.

**Invitation statuses:** `PENDING`, `ACCEPTED` only. There is no expiry on invitations; they remain valid indefinitely until accepted.

**Duplicate guards:**
- Cannot invite an email that already has a registered account.
- Cannot send a second invite to an email that already has a `PENDING` invite.

### Technical Direction

- Add invite revocation (`DELETE /api/v1/admin/invitations/{id}`) when admins need to cancel outstanding invites.
- Add pagination to `GET /api/v1/admin/invitations` for audit visibility.
- If invite expiry is required in the future, add `expiresAt` to `HManagerInvitation` and introduce an `EXPIRED` status — the current model does not need it.

---

## 9. HR-Driven Stage Transitions (No AI Auto-Advance)

### Problem

The previous flow had the AI screening service automatically advancing applications past `SCREENING` based on thresholds (`autoPassThreshold` and `autoRejectThreshold`). This made the AI a silent decision-maker — exactly what HireFlow's decision principles forbid.

### Current Plan

- After AI screening finishes, applications **stay in `SCREENING`**. The AI never advances or rejects an application by itself.
- The threshold comparison still runs — its only effect is to write a `ScreeningRecommendation` flag onto `AiScreeningResult`:

  | Recommendation | Condition |
  |---|---|
  | `AUTO_PASS` | `matchPercentage >= job.autoPassThreshold` |
  | `AUTO_REJECT` | `matchPercentage <  job.autoRejectThreshold` |
  | `MANUAL_REVIEW` | between the two thresholds |
  | `PENDING` | `matchPercentage` not yet computed |

- Admins and hiring managers see the recommendation flag and filter the applicant list with it: `GET /api/v1/applications/jobs/{jobId}?recommendation=AUTO_PASS` (or `AUTO_REJECT`, `MANUAL_REVIEW`).
- HR then triggers the stage change manually:
  - **Single:** `PATCH /api/v1/applications/{id}/stage` with `{ targetStage, reason }`
  - **Bulk:** `PATCH /api/v1/applications/stage/bulk` with `{ applicationIds[], targetStage, reason }`
- Only the HR-triggered endpoints publish the `APPLICATION_STAGE_UPDATED` Kafka notification. The AI screening pipeline no longer publishes stage-change notifications.

### Technical Direction

- Allowed forward transitions (centralised in `ApplicationStageTransitions`):
  - `SCREENING → INTERVIEW_SCHEDULED | REJECTED`
  - `INTERVIEW_SCHEDULED → OFFER_SENT | REJECTED`
  - `OFFER_SENT → HIRED | REJECTED`
  - `HIRED` and `REJECTED` are terminal.
- Each transition appends to the `StageUpdate` audit trail with the actor's email and supplied reason.
- The bulk endpoint is best-effort: it returns `{ requested, succeeded, failed, updatedApplicationIds[], failures[] }` so HR sees per-id outcomes without rolling back the whole batch on one bad row.
- Stage-update persistence is split from notification publishing per the same-class proxy rule: `ApplicationStageUpdatePersistence` runs the `@Transactional` write; `ApplicationServiceImpl` invokes the `@Async` notification producer after the transaction commits.
- The Q&A section (`application_answers`) is returned in `GET /api/v1/applications/{id}` so HR can review applicant responses before pulling the trigger. **AI never reads these answers.**

---

## Part 2: General/Strategic Alignment
## 10. AI Inconsistency Scoring - detect inconsistency

### Problem

AI can help detect inconsistencies across resumes, assessments, interviews, but inconsistency detection must be explainable and fair.

### Current Plan

- AI screening explains matched and unmatched skills.
- No full cross-source inconsistency score is currently documented.

- Compare claims across (AI scope, current build):
  - applicant skill list
  - resume summary
- Q&A answers and interview feedback are explicitly **excluded** from the AI inconsistency check — they belong to the human review section.
- Future expansion may add project descriptions and interview feedback once those signals are structured, but Q&A will remain human-only.
- Flag contradictions such as:
  - claiming a skill not supported by the resume
  - claimed seniority not matching resume evidence
- Show inconsistency as a review flag, not an automatic rejection.
- Event-driven stage updates notify candidates when their status changes.
- `StageUpdate` provides an append-only audit history.
- AI screening should expose matched skills, unmatched skills, match percentage, and explanation.
- Interview scoring uses structured criteria before offer decisions.

- Add candidate-facing status timelines.
- Show assessment completion status and expected review windows.
- Make rejection feedback structured and respectful without exposing internal-only risk flags.
- Add offer deadline reminders and candidate response tracking.

### Technical Direction

- Build an inconsistency scoring service that emits:
  - score
  - severity
  - evidence
  - explanation
  - recommended human review action
- Keep scoring thresholds configurable.
- Keep candidate-visible explanations separate from internal HR risk analysis.
- Preserve every stage transition for auditability.
- Require structured evidence before major hiring stage changes.
- Current basic implementation stores inconsistency as a review flag on `AiScreeningResult`; it does not directly reject applicants.
- Application stage/action changes are published to the Notification service as Kafka notification events.
- The Notification service is responsible for broadcasting applicant-facing updates over Server-Sent Events.

---

## 11. Decision Principles

- AI supports hiring decisions; it does not silently make final decisions.
- Every score that affects ranking must be explainable.
- Every automated flag must include evidence.
- Historical decisions must remain auditable even as AI prompts, scoring logic, and models evolve.
