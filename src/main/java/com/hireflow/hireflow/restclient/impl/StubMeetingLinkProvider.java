package com.hireflow.hireflow.restclient.impl;

import com.hireflow.hireflow.enums.MeetingProvider;
import com.hireflow.hireflow.restclient.MeetingLinkProvider;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Stub meeting link provider. Generates a Meet-shaped URL with a random
 * 10-character code (xxx-xxxx-xxx) — visually indistinguishable from a real
 * Google Meet link but never connects anywhere.
 *
 * Replace with a real provider when production conferencing is configured.
 */
@Component
public class StubMeetingLinkProvider implements MeetingLinkProvider {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    @Override
    public MeetingProvider provider() {
        return MeetingProvider.GOOGLE_MEET;
    }

    @Override
    public String createMeetingLink(String applicationId, Instant startTime, Instant endTime, String timezone, String organizerEmail) {
        return "https://meet.google.com/" + randomSegment(3) + "-" + randomSegment(4) + "-" + randomSegment(3);
    }

    private String randomSegment(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
