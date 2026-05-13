# Application Submission Flow

End-to-end flow of what happens when an applicant submits an application to a job — from the `POST /api/v1/applications/jobs/{jobId}` request through AI screening to the HR-driven stage transition.

---

## 1. Actors and Services

| Actor / Service | Role |
|---|---|
| **Applicant** | Submits the application via the hireflow REST API |
| **Hireflow Application Service** | Owns the database; persists the application; publishes/consumes Kafka events |
| **AI Screening Service** | Stateless worker; runs 4 parallel screening stages; owns no DB |
| **Notification Service** | Stateless worker; sends email + SSE for stage-change events |
| **Admin / HR Manager** | Reviews screening results and manually advances or rejects applications |

---

## 2. Step 1 — Submission

```
Applicant ──POST /api/v1/applications/jobs/{jobId}──▶ hireflow
   payload: { answers: [{ questionId, answer }, ...] }
```

Inside `ApplicationServiceImpl.applyToJob`:

1. `ApplicationSubmissionPersistence.submit` (`@Transactional`):
   - Loads applicant + job + resume profile
   - Rejects if the job is not `OPEN`
   - Rejects if the applicant already applied to this job
   - Validates every job question has an answer
   - Creates the `Application` with `stage = SCREENING`
   - Writes two `StageUpdate` audit rows: `null → APPLIED` and `APPLIED → SCREENING`
   - Persists each `ApplicationAnswer` with `questionSnapshot` (frozen question text)
   - Returns `ApplicationSubmissionResult { response, event }`
2. After the transaction commits, the orchestrator publishes:
   - `ApplicationSubmittedEvent` → `application-submitted` topic (Kafka)
   - Two `EmailNotificationEvent` → `application-stage-updated` topic (APPLIED + SCREENING transitions)

> **Important:** The `ApplicationSubmittedEvent` carries job context, job skills, applicant skills, resume summary, and the threshold values — **but NOT the applicant's Q&A answers**. Q&A is reserved for human review and never leaves the Application Service DB.

---

## 3. Step 2 — Parallel AI Screening

The single `application-submitted` topic is consumed by **4 independent consumer groups** in the AI Screening Service. Each group gets its own full copy of the event from Kafka, so the 4 stages run in parallel and a failure in one does not block another.

```
hireflow.application.submitted.v1
        │
   ┌────┴────────────────────┬──────────────────────┬───────────────────┐
   ▼                         ▼                      ▼                   ▼
ResumeAnalysisConsumer  ProjectConsistencyConsumer  InconsistencyConsumer  MatchSummaryConsumer
(group: ai-screening-resume-analysis)
                        (group: ai-screening-project-consistency)
                                                (group: ai-screening-inconsistency-review)
                                                                       (group: ai-screening-match-summary)
        │                         │                      │                   │
        ▼                         ▼                      ▼                   ▼
hireflow.screening.resume.v1  hireflow.screening.project.v1  hireflow.screening.inconsistency.v1  hireflow.screening.completed.v1
```

### Stage Inputs (what each AI screener actually reads)

| Stage | Reads | Does NOT read |
|---|---|---|
| Resume Analysis | `jobSkills`, `applicantSkills`, `resumeSummary` | Q&A |
| Project Consistency | `jobSkills`, **resume summary only** | Q&A, applicant answers |
| Inconsistency Review | `applicantSkills`, **resume summary only** | Q&A, applicant answers |
| Match Summary | `jobSkills`, `applicantSkills`, `resumeSummary` | Q&A |

Each `OpenAi*` screener has a deterministic `Basic*` twin. The OpenAI bean is `@Primary`; if its HTTP call fails or returns malformed JSON, the OpenAI screener catches the exception and delegates to the injected `Basic*` fallback — the pipeline never breaks because of a provider outage.

---

## 4. Step 3 — Result Persistence on Hireflow

Hireflow consumes all 4 result topics. Per-stage events update only their own slice of `AiScreeningResult` via null-safe partial-merge setters (see `ApplicationScreeningPersistence`).

```
hireflow.screening.resume.v1        ──▶ ResumeAnalysisCompletedConsumer       ──▶ applyResumeAnalysis (sets resume-* fields)
hireflow.screening.project.v1       ──▶ ProjectConsistencyCompletedConsumer   ──▶ applyProjectConsistency
hireflow.screening.inconsistency.v1 ──▶ InconsistencyReviewCompletedConsumer  ──▶ applyInconsistencyReview
hireflow.screening.completed.v1     ──▶ ScreeningCompletedConsumer            ──▶ finalizeScreening
```

`finalizeScreening` is where the **threshold check** runs — but it does NOT change the application stage. It writes a `ScreeningRecommendation` enum onto `AiScreeningResult`:

| matchPercentage | Recommendation |
|---|---|
| `>= job.autoPassThreshold` | `AUTO_PASS` |
| `<  job.autoRejectThreshold` | `AUTO_REJECT` |
| in between | `MANUAL_REVIEW` |
| null | `PENDING` |

After all 4 events land, the application is still in `SCREENING`. Nothing has been auto-advanced or auto-rejected. **No stage-change notification is published by the AI pipeline.**

---

## 5. Step 4 — HR Review

The hiring manager / admin opens the applicant list for the job:

```
GET /api/v1/applications/jobs/{jobId}?recommendation=AUTO_PASS&page=0&size=20
```

The `recommendation` query parameter is optional and accepts `AUTO_PASS`, `MANUAL_REVIEW`, `AUTO_REJECT`, or `PENDING`. HR uses it to triage the list:

- "Show me everyone the AI thinks looks like a pass" → `recommendation=AUTO_PASS`
- "Show me everyone the AI thinks looks like a reject" → `recommendation=AUTO_REJECT`
- Omit the parameter to see the full list with no filter

To inspect a single applicant in detail:

```
GET /api/v1/applications/{applicationId}
```

The response includes:
- `stage` (currently `SCREENING`)
- `screeningResult` — full AI output: matched/unmatched skills, all 4 stage scores, narrative summary, **and `recommendation`**
- `answers[]` — the candidate-facing Q&A, with `question`, `answer`, and `createdAt`
- `stageUpdates[]` — full audit trail
- `resumeProfile` — embedded resume details

The Q&A section is shown to HR for human review. It is not derived from any AI signal and is intentionally separate from the `screeningResult` block.

---

## 6. Step 5 — HR-Triggered Stage Transition

Once HR has reviewed the applicant, they advance or reject the application explicitly.

### Single Update

```
PATCH /api/v1/applications/{applicationId}/stage
{
  "targetStage": "INTERVIEW_SCHEDULED",
  "reason": "Strong Java match, good Q&A answers"
}
```

### Bulk Update

```
PATCH /api/v1/applications/stage/bulk
{
  "applicationIds": ["app-1", "app-2", "app-3"],
  "targetStage": "REJECTED",
  "reason": "Closing the auto-reject batch"
}
```

Bulk response:
```json
{
  "requested": 3,
  "succeeded": 2,
  "failed": 1,
  "updatedApplicationIds": ["app-1", "app-2"],
  "failures": [
    { "applicationId": "app-3", "reason": "Stage transition not allowed: HIRED → REJECTED" }
  ]
}
```

Bulk updates are best-effort per-id; a single failure does not roll back the whole batch.

### Allowed Transitions

Centralised in `ApplicationStageTransitions`:

| From | Allowed To |
|---|---|
| `SCREENING` | `INTERVIEW_SCHEDULED`, `REJECTED` |
| `INTERVIEW_SCHEDULED` | `OFFER_SENT`, `REJECTED` |
| `OFFER_SENT` | `HIRED`, `REJECTED` |
| `HIRED` | (terminal) |
| `REJECTED` | (terminal) |

Per HR stage update:
1. `ApplicationStageUpdatePersistence.updateStage` (`@Transactional`):
   - Loads the application scoped to the caller's company
   - Validates the transition is allowed
   - Updates `stage`
   - Appends to `StageUpdate` with the actor's email + reason
   - Returns the `EmailNotificationEvent` payload (not yet published)
2. The orchestrator publishes the notification via `NotificationEventProducer.publishApplicationStageUpdateAsync` after the transaction commits.

The Notification Service then sends the applicant the stage-change email and pushes an SSE event for real-time UI updates.

---

## 7. End-to-End Picture

```
APPLICANT
   │
   │ POST /api/v1/applications/jobs/{jobId}
   ▼
HIREFLOW APPLICATION SERVICE ───────────────────────────────────────────────┐
   │  • persist Application (stage = SCREENING)                              │
   │  • persist ApplicationAnswers (human review section, never sent to AI)  │
   │  • publish ApplicationSubmittedEvent (no answers)                       │
   │  • publish EmailNotificationEvent ×2 (APPLIED, SCREENING)               │
   │                                                                         │
   ▼                                                                         │
KAFKA: application-submitted ──▶ AI SCREENING SERVICE                        │
                                  (4 consumer groups, 4 parallel stages)     │
                                                                             │
                                       │                                     │
                                       ▼                                     │
KAFKA: 4 result topics ─────────────────────────────────────────────────────▶│
                                                                             │
   │  • partial-merge each stage into AiScreeningResult                      │
   │  • finalizeScreening computes ScreeningRecommendation                   │
   │  • Application stays in SCREENING (no auto-advance, no notification)    │
   ▼                                                                         │
HR MANAGER                                                                   │
   │  GET /api/v1/applications/jobs/{jobId}?recommendation=AUTO_PASS         │
   │  GET /api/v1/applications/{id}  (includes screeningResult + answers)    │
   │                                                                         │
   │  reviews the Q&A and AI recommendation, then:                           │
   │                                                                         │
   ▼                                                                         │
PATCH /api/v1/applications/{id}/stage  OR  PATCH /api/v1/applications/stage/bulk
   │                                                                         │
   ▼                                                                         │
HIREFLOW APPLICATION SERVICE                                                 │
   │  • validate transition, update stage, append StageUpdate                │
   │  • publish APPLICATION_STAGE_UPDATED notification                       │
   │                                                                         │
   ▼                                                                         │
NOTIFICATION SERVICE ────────────────────────────────────────────────────────┘
   • email the applicant
   • SSE push for real-time UI
```

---

## 8. Invariants

- **AI never advances or rejects an application.** Stage transitions out of `SCREENING` are HR-only.
- **AI never reads applicant Q&A.** The `ApplicationSubmittedEvent` excludes answers; the AI prompts explicitly state Q&A is reserved for human reviewers.
- **`ScreeningRecommendation` is informational only.** It exists to help HR filter the applicant list. It does not gate any automatic action.
- **Per-stage AI results are partial-merge.** Each of the 4 events writes only its own fields onto `AiScreeningResult`; one slow or failing stage cannot overwrite another stage's output.
- **Stage transitions are append-only.** Every change writes a row to `StageUpdate` with `previousStage`, `currentStage`, `reason`, and `actor`.
- **Notifications fire only after persistence commits.** Both submission and stage updates publish their Kafka notifications from outside the `@Transactional` boundary so an applicant is never emailed about a state that ends up rolled back.
