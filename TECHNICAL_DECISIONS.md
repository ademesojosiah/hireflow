# HireFlow v3.0 - Technical Decisions and Product Direction

This document records the technical decisions, current solutions, and future plans for HireFlow. It should stay practical: every product problem should map to an implemented or planned technical response.

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

Each problem below is tracked with the current solution and future plan.

---

## 3. Hiring Manager Recommendation

### Problem

HR managers need a clear best-candidate recommendation, not just a list of applicants. Ranking should explain why a candidate is recommended and what risks remain.

### Current Solution

- Store resume profile data in structured fields.
- Use AI resume analysis to extract skills, experience, education, and match indicators.
- Surface matched and unmatched skills against the job requirements.
- Preserve stage transitions through an append-only `StageUpdate` audit trail.
- Allow HR managers to compare top candidates side by side.

### Future Plan

- Add a hiring manager recommendation score that combines:
  - live assessment score
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

### Current Solution

- Job descriptions and requirements provide the base criteria for resume matching.
- Interview scorecards provide structured evaluation after interviews.

### Future Plan

- Allow job posters to create technical questions while posting a job.
- Support question types:
  - short answer
  - multiple choice
  - coding challenge link
  - project explanation
  - scenario-based answer
- Allow AI to suggest questions from the job description, but require poster approval before publishing.
- Attach questions to a job version so later job edits do not silently change an active applicant's assessment.

### Technical Direction

- Add entities such as `JobQuestion`, `QuestionType`, and `ApplicantAnswer`.
- Store poster-authored questions separately from AI-suggested drafts.
- Version questions per job posting to preserve assessment fairness.

---

## 5. Disallow Copy and Paste by Applicants

### Problem

Applicants can paste AI-generated or copied answers into assessments, reducing signal quality.

### Current Solution

- No strict anti-paste enforcement is currently documented.
- Resume upload and profile submission are normal form-based workflows.

### Future Plan

- Disable paste events in live assessment answer fields.
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

## 6. AI Resume Analysis

### Problem

Hiring teams need fast resume screening, but candidates should not be rejected by an unexplained black box.

### Current Solution

- Resume analysis extracts structured profile information.
- AI screening should surface:
  - matched skills
  - unmatched skills
  - match percentage
  - short explanation

### Future Plan

- Add stronger parsing for projects, certifications, tools, seniority, and role-specific experience.
- Compare resume claims directly against job requirements.
- Return confidence levels per extracted field.
- Flag missing or weak evidence without automatically rejecting the applicant.

### Technical Direction

- Keep raw resume metadata separate from normalized profile fields.
- Store AI output as explainable structured data.
- Add validation around AI-generated fields before using them in ranking.

---

## 7. Project Consistency

### Problem

Applicants may list projects that do not align with their claimed skills, seniority, or role experience.

### Current Solution

- Project data can be collected from resumes and applicant profiles.
- Resume analysis can identify skills and project descriptions.

### Future Plan

- Score whether listed projects support the applicant's claimed skills.
- Detect mismatches such as:
  - senior-level skill claims with only beginner projects
  - project descriptions that do not mention the claimed stack
  - repeated vague project wording
  - projects unrelated to the target role
- Ask applicants follow-up technical questions about listed projects during assessment.

### Technical Direction

- Add a project consistency scoring component to the recommendation service.
- Store evidence links between skills and project descriptions.
- Use consistency scores as one input into HR recommendations, not as a standalone rejection rule.

---

## 8. Live Assessments

### Problem

Static resumes are not enough. Hiring teams need evidence of how applicants solve problems in real time.

### Current Solution

- Interview workflow supports structured scoring.
- Assessment-specific live workflow is not yet fully documented.

### Future Plan

- Add timed live assessments tied to each job posting.
- Support job-poster questions and role-specific tasks.
- Track answer drafts, submission time, integrity events, and scoring.
- Allow AI-assisted grading with human review for final hiring decisions.

### Technical Direction

- Add an assessment module with entities such as `Assessment`, `AssessmentSession`, `AssessmentSubmission`, and `AssessmentIntegrityEvent`.
- Separate assessment scoring from interview scoring.
- Keep assessment state transitions explicit: `NOT_STARTED`, `IN_PROGRESS`, `SUBMITTED`, `SCORED`, `REVIEWED`.

---

## 10. AI Inconsistency Scoring

### Problem

AI can help detect inconsistencies across resumes, assessments, interviews, and GitHub data, but inconsistency detection must be explainable and fair.

### Current Solution

- AI screening explains matched and unmatched skills.
- No full cross-source inconsistency score is currently documented.

### Future Plan

- Compare claims across:
  - resume
  - applicant profile
  - project descriptions
  - live assessment answers
  - interview feedback
  - optional GitHub signals
- Flag contradictions such as:
  - claiming a framework but failing basic questions about it
  - listing a project stack that does not appear in the project explanation
  - GitHub repositories that do not support claimed public project work
- Show inconsistency as a review flag, not an automatic rejection.

### Technical Direction

- Build an inconsistency scoring service that emits:
  - score
  - severity
  - evidence
  - explanation
  - recommended human review action
- Keep scoring thresholds configurable.
- Log score versions so future model changes do not rewrite historical hiring decisions.

---

## 11. Candidate Transparency and Process Consistency

### Problem

Candidates experience silent application periods, opaque AI screening, inconsistent interviews, and unclear offer timelines.

### Current Solution

- Event-driven stage updates notify candidates when their status changes.
- `StageUpdate` provides an append-only audit history.
- AI screening should expose matched skills, unmatched skills, match percentage, and explanation.
- Interview scoring uses structured criteria before offer decisions.

### Future Plan

- Add candidate-facing status timelines.
- Show assessment completion status and expected review windows.
- Make rejection feedback structured and respectful without exposing internal-only risk flags.
- Add offer deadline reminders and candidate response tracking.

### Technical Direction

- Keep candidate-visible explanations separate from internal HR risk analysis.
- Preserve every stage transition for auditability.
- Require structured evidence before major hiring stage changes.

---

## 12. Decision Principles

- AI supports hiring decisions; it does not silently make final decisions.
- Every score that affects ranking must be explainable.
- Every automated flag must include evidence.
- Optional external signals, such as GitHub, should only improve confidence and should not unfairly exclude candidates.
- Historical decisions must remain auditable even as AI prompts, scoring logic, and models evolve.
