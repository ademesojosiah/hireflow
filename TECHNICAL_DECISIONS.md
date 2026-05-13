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

- Add applicant answer capture when the application assessment flow is introduced.
- Use the stored answer guide for AI-assisted grading or HR review.
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
- The combined `matchPercentage` remains the only score used for automatic threshold movement.

- Add stronger parsing for projects, certifications, tools, seniority, and role-specific experience.
- Compare resume claims directly against job requirements.
- Return confidence levels per extracted field.
- Flag missing or weak evidence without automatically rejecting the applicant.

### Technical Direction

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
- Current basic implementation uses available resume summary/profile evidence as a placeholder until structured project extraction exists.

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
| Application Service | YES â€” owns all tables | Persistence authority; emits domain events |
| AI Screening Service | NO | Consumes `ApplicationSubmitted`; runs AI; publishes `ScreeningCompleted` / `ScreeningFailed` |
| Notification Service | NO | Consumes stage-change events; sends email/SMS; publishes `NotificationSent` |

**Reliability without a DB:** Retry durability for the stateless services is provided by Kafka â€” retry topics with exponential backoff, dead-letter topics (DLTs) for poison messages. If AI screening fails, the AI service publishes `ScreeningFailed` and the Application Service marks the result as failed. No local DB required.

**When to revisit:** Add a database to a stateless service only if its data access patterns require independent querying at scale, or compliance mandates a separate audit store with long-term retention.

---

## Part 2: General/Strategic Alignment
## 10. AI Inconsistency Scoring - detect inconsistency

### Problem

AI can help detect inconsistencies across resumes, assessments, interviews, but inconsistency detection must be explainable and fair.

### Current Plan

- AI screening explains matched and unmatched skills.
- No full cross-source inconsistency score is currently documented.

- Compare claims across:
  - resume
  - applicant profile
  - project descriptions
  - assessment answers
  - interview feedback
- Flag contradictions such as:
  - claiming a framework but failing basic questions about it
  - listing a project stack that does not appear in the project explanation
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
