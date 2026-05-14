package com.hireflow.hireflow.restclient.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireflow.hireflow.exception.CustomException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleCalendarMeetingLinkProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServer server;
    private GoogleCalendarMeetingLinkProvider provider;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();

        provider = new GoogleCalendarMeetingLinkProvider(objectMapper);
        setField("serviceAccountEmail", "calendar-service@example.iam.gserviceaccount.com");
        setField("privateKeyPem", privateKeyPem());
        setField("delegatedUserEmail", "");
        setField("delegateToOrganizer", true);
        setField("calendarId", "primary");
        setField("sendUpdates", "all");
        setField("tokenUri", baseUrl + "/token");
        setField("eventsBaseUri", baseUrl + "/calendar/v3/calendars");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Creates a Calendar event with Meet conference data and returns Google's hangoutLink")
    void createMeetingLink_createsGoogleMeetEvent() throws Exception {
        AtomicReference<Map<String, String>> tokenForm = new AtomicReference<>();
        AtomicReference<String> eventAuthorization = new AtomicReference<>();
        AtomicReference<String> eventQuery = new AtomicReference<>();
        AtomicReference<JsonNode> eventBody = new AtomicReference<>();

        server.createContext("/token", exchange -> {
            tokenForm.set(parseForm(readBody(exchange)));
            respondJson(exchange, 200, "{\"access_token\":\"access-token-123\",\"token_type\":\"Bearer\"}");
        });
        server.createContext("/calendar/v3/calendars/primary/events", exchange -> {
            eventAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            eventQuery.set(exchange.getRequestURI().getRawQuery());
            eventBody.set(objectMapper.readTree(readBody(exchange)));
            respondJson(exchange, 200, "{\"hangoutLink\":\"https://meet.google.com/abc-defg-hij\"}");
        });

        String link = provider.createMeetingLink(
                "application-1",
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T11:00:00Z"),
                "Africa/Lagos",
                "maya@example.com"
        );

        assertThat(link).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(tokenForm.get())
                .containsEntry("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                .containsKey("assertion");
        assertThat(eventAuthorization.get()).isEqualTo("Bearer access-token-123");
        assertThat(eventQuery.get()).contains("conferenceDataVersion=1").contains("sendUpdates=all");
        assertThat(eventBody.get().at("/conferenceData/createRequest/conferenceSolutionKey/type").asText())
                .isEqualTo("hangoutsMeet");
        assertThat(eventBody.get().at("/start/timeZone").asText()).isEqualTo("Africa/Lagos");
        assertThat(eventBody.get().at("/end/timeZone").asText()).isEqualTo("Africa/Lagos");
        assertThat(eventBody.get().at("/attendees/0/email").asText()).isEqualTo("maya@example.com");
    }

    @Test
    @DisplayName("Fails when Google creates an event without returning hangoutLink")
    void createMeetingLink_requiresHangoutLink() {
        server.createContext("/token", exchange ->
                respondJson(exchange, 200, "{\"access_token\":\"access-token-123\",\"token_type\":\"Bearer\"}"));
        server.createContext("/calendar/v3/calendars/primary/events", exchange ->
                respondJson(exchange, 200, "{\"htmlLink\":\"https://calendar.google.com/event\"}"));

        assertThatThrownBy(() -> provider.createMeetingLink(
                "application-1",
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T11:00:00Z"),
                "Africa/Lagos",
                "maya@example.com"
        ))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("did not return a Google Meet link");
    }

    @Test
    @DisplayName("Fails fast when service account configuration is missing")
    void createMeetingLink_requiresServiceAccountConfig() throws Exception {
        setField("serviceAccountEmail", "");

        assertThatThrownBy(() -> provider.createMeetingLink(
                "application-1",
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T11:00:00Z"),
                "Africa/Lagos",
                "maya@example.com"
        ))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("hireflow.google.calendar.service-account-email");
    }

    private String privateKeyPem() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n";
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = GoogleCalendarMeetingLinkProvider.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(provider, value);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private Map<String, String> parseForm(String body) {
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                form.put(
                        URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }
        return form;
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
