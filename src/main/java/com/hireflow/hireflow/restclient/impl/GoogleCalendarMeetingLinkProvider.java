package com.hireflow.hireflow.restclient.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.hireflow.enums.MeetingProvider;
import com.hireflow.hireflow.exception.CustomException;
import com.hireflow.hireflow.restclient.MeetingLinkProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleCalendarMeetingLinkProvider implements MeetingLinkProvider {

    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";
    private static final String JWT_BEARER_GRANT = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${hireflow.google.calendar.service-account-email:}")
    private String serviceAccountEmail;

    @Value("${hireflow.google.calendar.private-key:}")
    private String privateKeyPem;

    @Value("${hireflow.google.calendar.delegated-user-email:}")
    private String delegatedUserEmail;

    @Value("${hireflow.google.calendar.delegate-to-organizer:true}")
    private boolean delegateToOrganizer;

    @Value("${hireflow.google.calendar.calendar-id:primary}")
    private String calendarId;

    @Value("${hireflow.google.calendar.send-updates:all}")
    private String sendUpdates;

    @Value("${hireflow.google.calendar.oauth-token-uri:https://oauth2.googleapis.com/token}")
    private String tokenUri;

    @Value("${hireflow.google.calendar.events-base-uri:https://www.googleapis.com/calendar/v3/calendars}")
    private String eventsBaseUri;

    @Override
    public MeetingProvider provider() {
        return MeetingProvider.GOOGLE_MEET;
    }

    @Override
    public String createMeetingLink(String applicationId, Instant startTime, Instant endTime, String timezone, String organizerEmail) {
        requireConfigured(serviceAccountEmail, "hireflow.google.calendar.service-account-email");
        requireConfigured(privateKeyPem, "hireflow.google.calendar.private-key");

        String accessToken = fetchAccessToken(resolveDelegatedUser(organizerEmail));
        JsonNode event = insertCalendarEvent(accessToken, applicationId, startTime, endTime, timezone, organizerEmail);
        JsonNode hangoutLink = event.get("hangoutLink");
        if (hangoutLink == null || hangoutLink.asText().isBlank()) {
            throw new CustomException("Google Calendar created the event but did not return a Google Meet link");
        }
        return hangoutLink.asText();
    }

    private String fetchAccessToken(String subject) {
        try {
            String assertion = buildJwtAssertion(subject);
            String body = "grant_type=" + urlEncode(JWT_BEARER_GRANT)
                    + "&assertion=" + urlEncode(assertion);
            HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUri))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CustomException("Google OAuth token request failed with status " + response.statusCode());
            }
            JsonNode json = objectMapper.readTree(response.body());
            JsonNode accessToken = json.get("access_token");
            if (accessToken == null || accessToken.asText().isBlank()) {
                throw new CustomException("Google OAuth token response did not include access_token");
            }
            return accessToken.asText();
        } catch (IOException ex) {
            throw new CustomException("Google OAuth token request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CustomException("Google OAuth token request was interrupted", ex);
        }
    }

    private JsonNode insertCalendarEvent(
            String accessToken,
            String applicationId,
            Instant startTime,
            Instant endTime,
            String timezone,
            String organizerEmail
    ) {
        try {
            Map<String, Object> eventBody = Map.of(
                    "summary", "HireFlow interview",
                    "description", "Interview scheduled from HireFlow for application " + applicationId,
                    "start", Map.of("dateTime", startTime.toString(), "timeZone", timezone),
                    "end", Map.of("dateTime", endTime.toString(), "timeZone", timezone),
                    "attendees", List.of(Map.of("email", organizerEmail)),
                    "conferenceData", Map.of(
                            "createRequest", Map.of(
                                    "requestId", UUID.randomUUID().toString(),
                                    "conferenceSolutionKey", Map.of("type", "hangoutsMeet")
                            )
                    )
            );
            URI uri = URI.create(eventsBaseUri + "/" + pathEncode(calendarId)
                    + "/events?conferenceDataVersion=1&sendUpdates=" + urlEncode(sendUpdates));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(eventBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CustomException("Google Calendar event creation failed with status " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new CustomException("Google Calendar event creation failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CustomException("Google Calendar event creation was interrupted", ex);
        }
    }

    private String buildJwtAssertion(String subject) {
        try {
            Instant now = Instant.now();
            var builder = JWT.create()
                    .withIssuer(serviceAccountEmail)
                    .withAudience(tokenUri)
                    .withIssuedAt(Date.from(now))
                    .withExpiresAt(Date.from(now.plusSeconds(3600)))
                    .withClaim("scope", CALENDAR_SCOPE);
            if (subject != null && !subject.isBlank()) {
                builder.withSubject(subject);
            }
            return builder.sign(Algorithm.RSA256(null, parsePrivateKey(privateKeyPem)));
        } catch (GeneralSecurityException ex) {
            throw new CustomException("Invalid Google service account private key", ex);
        }
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        String normalized = pem.replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private String resolveDelegatedUser(String organizerEmail) {
        if (delegatedUserEmail != null && !delegatedUserEmail.isBlank()) {
            return delegatedUserEmail;
        }
        return delegateToOrganizer ? organizerEmail : null;
    }

    private void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new CustomException("Missing required Google Calendar configuration: " + property);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String pathEncode(String value) {
        return urlEncode(value).replace("+", "%20");
    }
}
