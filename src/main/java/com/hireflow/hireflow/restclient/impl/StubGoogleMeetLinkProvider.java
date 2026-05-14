package com.hireflow.hireflow.restclient.impl;

import com.hireflow.hireflow.enums.MeetingProvider;
import com.hireflow.hireflow.restclient.MeetingLinkProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Placeholder Google Meet link generator. Produces a URL in the shape
 * {@code https://meet.google.com/abc-defg-hij} without actually creating a calendar event.
 * Swap with a real OAuth + Google Calendar API implementation when ready — same interface,
 * same call site.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hireflow.meeting.provider", havingValue = "stub", matchIfMissing = true)
public class StubGoogleMeetLinkProvider implements MeetingLinkProvider {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public MeetingProvider provider() {
        return MeetingProvider.GOOGLE_MEET;
    }

    @Override
    public String createMeetingLink(String applicationId, Instant startTime, Instant endTime, String timezone, String organizerEmail) {
        String code = randomSegment(3) + "-" + randomSegment(4) + "-" + randomSegment(3);
        String link = "https://meet.google.com/" + code;
        log.info("Generated stub Google Meet link for application {} ({} → {}): {}",
                applicationId, startTime, endTime, link);
        return link;
    }

    private String randomSegment(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
