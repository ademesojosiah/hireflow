# HireFlow v3.0 — Technical Decisions & Issues Faced

---

## Part 1: Technical Decisions

### 1. Temporal Fields: `Instant` vs `LocalDateTime` ✓

Use `java.time.Instant` for all audit timestamps (`createdAt`, `updatedAt`).

**Why:** `LocalDateTime` is timezone-naive and ambiguous in distributed systems. `Instant` is UTC-anchored, globally unambiguous, and database-portable. Required for correct sorting and comparison across timezones.

```java
@CreationTimestamp
private Instant createdAt;

@UpdateTimestamp
private Instant updatedAt;
```

---

### 2. `@Async` Placement: After Transactional Boundaries, Never Inside ✓

`@Async` methods must be called *after* `@Transactional` methods complete. Never nest async inside transactional.

**Why:** Transactions hold database sessions. Async threads inherit the tx context but cannot safely use it once the original thread commits/rollbacks. Results: deadlocks, lazy-loading exceptions, "connection already closed" errors.

**Correct Pattern:**
```java
@Transactional
public User updateUser(User user) {
    return userRepository.save(user);  // Tx commits here
}

// Called AFTER updateUser() returns:
@Async
public void sendWelcomeAsync(User user) {
    emailService.send(user.getEmail(), ...);  // No tx context
}
```

---

## Part 2: General/Strategic Alignment

### The Problem: "Recruitment Anxiety Gap"

Modern hiring is broken. Candidates face:
- **Silent application purgatory**: Days/weeks with zero feedback on application status
- **Opaque AI screening**: Rejected by algorithms they don't understand, no clarity on *why*
- **Inconsistent interviews**: Multiple interviewers, conflicting signals, no visible scorecard
- **Ghosting on offers**: No timeline clarity or feedback

**Result:** Candidates drop out mid-process due to uncertainty. Hiring teams don't know why. Both parties operate on information scarcity.

---

### HireFlow v3.0 Solution: Three Pillars

#### 1. Event-Driven Transparency
Every stage transition (`APPLIED` → `SCREENING` → `INTERVIEW_SCHEDULED` → `OFFER_SENT` → `HIRED`) triggers immediate notifications to the candidate and audit log.

- Candidate knows status at all times
- No silent periods
- Hiring team sees full transition history (`StageUpdate` append-only log)

#### 2. AI-Augmented but Explainable Screening
AI filters fast, but always surfaces explainability:
- **Matched Skills**: "Your resume mentions Java, Spring, MySQL — all required"
- **Unmatched Skills**: "This role prefers AWS, which we didn't find"
- **Match Percentage**: 0–100% score with brief narrative

No black-box rejections. Hiring managers see the same explainability.

#### 3. Structured Interview Workflow with Live Scoring
Interviews are standardized:
- Google Meet link pre-generated
- 5-point scorecard (Technical, Behavioral, Communication, Culture Fit, Problem-Solving)
- All five criteria must be scored before offer can be sent

Advancement is *gated*. No offer without evidence.

---

### Problem-Solution Alignment

| **Problem** | **HireFlow Response** |
|---|---|
| Silent application purgatory | Event-driven notifications on every stage change; `StageUpdate` append-only log |
| Opaque AI rejection | `AIScreeningResult` surfaces matched/unmatched skills + match %; narrative included |
| Inconsistent interviews | `InterviewSlot` enforces 5-criteria scoresheet; no offer without all five scores |
| Ghosting on offers | Offer deadline included in email; candidate portal shows countdown |
| No audit trail | `StageUpdate` immutable; cannot delete/update; full transition history retained |
| Candidate abandonment | Transparency + feedback loops keep candidate engaged throughout process |
