package com.hireflow.hireflow.restclient;

import com.hireflow.hireflow.enums.MeetingProvider;

import java.time.Instant;

/**
 * Generates a meeting link for a scheduled interview. Implementations call out to the
 * conferencing provider (Google Meet, Zoom, etc.). Production wiring should replace the
 * stub with a real OAuth-backed integration.
 */
public interface MeetingLinkProvider {

    MeetingProvider provider();

    /**
     * Returns a fully-qualified meeting URL the interviewer and applicant can both join.
     *
     * @param applicationId application this interview is for (logged/tagged on the provider side)
     * @param startTime     when the meeting starts (UTC)
     * @param endTime       when the meeting ends (UTC)
     * @param organizerEmail the interviewer who owns the calendar event
     */
    String createMeetingLink(String applicationId, Instant startTime, Instant endTime, String organizerEmail);
}
