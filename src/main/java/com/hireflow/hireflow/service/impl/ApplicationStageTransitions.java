package com.hireflow.hireflow.service.impl;

import com.hireflow.hireflow.enums.ApplicationStage;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.hireflow.hireflow.enums.ApplicationStage.HIRED;
import static com.hireflow.hireflow.enums.ApplicationStage.INTERVIEW_SCHEDULED;
import static com.hireflow.hireflow.enums.ApplicationStage.OFFER_SENT;
import static com.hireflow.hireflow.enums.ApplicationStage.REJECTED;
import static com.hireflow.hireflow.enums.ApplicationStage.SCREENING;

/**
 * Allowed forward transitions for the generic HR-driven {@code PATCH /applications/{id}/stage} endpoint.
 *
 * INTERVIEW_SCHEDULED is intentionally absent from every allowed-to set — reaching that stage
 * requires a scheduled InterviewSlot, so callers must go through {@code POST /applications/{id}/interview}
 * instead. The interview endpoint sets the stage atomically with slot creation.
 *
 * REJECTED is reachable from any non-terminal stage; HIRED and REJECTED are terminal.
 */
final class ApplicationStageTransitions {

    private ApplicationStageTransitions() {}

    private static final Map<ApplicationStage, Set<ApplicationStage>> ALLOWED = Map.of(
            SCREENING, EnumSet.of(REJECTED),
            INTERVIEW_SCHEDULED, EnumSet.of(OFFER_SENT, REJECTED),
            OFFER_SENT, EnumSet.of(HIRED, REJECTED),
            HIRED, EnumSet.noneOf(ApplicationStage.class),
            REJECTED, EnumSet.noneOf(ApplicationStage.class)
    );

    static boolean isAllowed(ApplicationStage current, ApplicationStage target) {
        if (current == null || target == null) return false;
        if (current == target) return false;
        // INTERVIEW_SCHEDULED can only be reached through the interview endpoint, never the generic stage PATCH.
        if (target == INTERVIEW_SCHEDULED) return false;
        Set<ApplicationStage> allowed = ALLOWED.getOrDefault(current, EnumSet.noneOf(ApplicationStage.class));
        return allowed.contains(target);
    }
}
