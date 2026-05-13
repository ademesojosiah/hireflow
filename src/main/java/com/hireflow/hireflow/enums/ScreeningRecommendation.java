package com.hireflow.hireflow.enums;

/**
 * Threshold-derived recommendation surfaced to HR after AI screening completes.
 * It is purely informational — the AI never advances or rejects an application.
 * The hiring team filters the applicant list by this flag and applies stage transitions manually.
 *
 * Bucketing (against the job listing's autoPassThreshold / autoRejectThreshold):
 *   AUTO_PASS       — matchPercentage >= autoPassThreshold
 *   AUTO_REJECT     — matchPercentage <  autoRejectThreshold
 *   MANUAL_REVIEW   — between the two thresholds
 *   PENDING         — AI has not produced a final matchPercentage yet
 */
public enum ScreeningRecommendation {
    PENDING,
    AUTO_PASS,
    MANUAL_REVIEW,
    AUTO_REJECT
}
