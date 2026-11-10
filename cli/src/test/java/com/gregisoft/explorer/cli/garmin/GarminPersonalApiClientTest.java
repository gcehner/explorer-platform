package com.gregisoft.explorer.cli.garmin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GarminPersonalApiClientTest {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void exchangesApiKeyForPlainTextJwtUsingOfficialContract() throws Exception {
        String token = jwt(NOW.plus(Duration.ofHours(24)).getEpochSecond());
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<byte[]> body = new AtomicReference<>();
        startServer(exchange -> {
            method.set(exchange.getRequestMethod());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(exchange.getRequestBody().readAllBytes());
            respond(exchange, 200, token + "\n");
        });

        GarminAccessToken result = client(Duration.ofSeconds(2)).authenticate("test-key".toCharArray());

        assertEquals("POST", method.get());
        assertEquals("ApiKey test-key", authorization.get());
        assertEquals(0, body.get().length);
        assertEquals(token, result.value());
        assertEquals(NOW.plus(Duration.ofHours(24)), result.expiresAt());
        assertFalse(result.toString().contains(token));
    }

    @Test
    void rejectsInvalidKeyWithoutExposingResponseOrKey() throws Exception {
        startServer(exchange -> respond(
                exchange, 401, "invalid key test-key and server-secret"));

        GarminAuthenticationException exception = assertThrows(
                GarminAuthenticationException.class,
                () -> client(Duration.ofSeconds(2)).authenticate("test-key".toCharArray()));

        assertTrue(exception.getMessage().contains("invalid, expired, or revoked"));
        assertFalse(exception.getMessage().contains("test-key"));
        assertFalse(exception.getMessage().contains("server-secret"));
    }

    @Test
    void reportsTimeoutSafely() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(250);
                respond(exchange, 200, jwt(NOW.plusSeconds(3600).getEpochSecond()));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // The timed-out client may close the exchange before the response is written.
            }
        });

        GarminAuthenticationException exception = assertThrows(
                GarminAuthenticationException.class,
                () -> client(Duration.ofMillis(40)).authenticate("test-key".toCharArray()));

        assertTrue(exception.getMessage().contains("timed out"));
        assertFalse(exception.getMessage().contains("test-key"));
    }

    @Test
    void rejectsMalformedOrExpiredTokenResponses() throws Exception {
        startServer(exchange -> respond(exchange, 200, "not-a-jwt"));
        GarminAuthenticationException malformed = assertThrows(
                GarminAuthenticationException.class,
                () -> client(Duration.ofSeconds(2)).authenticate("key".toCharArray()));
        assertTrue(malformed.getMessage().contains("malformed"));

        server.stop(0);
        startServer(exchange -> respond(exchange, 200, jwt(NOW.minusSeconds(1).getEpochSecond())));
        GarminAuthenticationException expired = assertThrows(
                GarminAuthenticationException.class,
                () -> client(Duration.ofSeconds(2)).authenticate("key".toCharArray()));
        assertTrue(expired.getMessage().contains("expired"));
    }

    @Test
    void rejectsNonHttpsNonLoopbackEndpoints() {
        assertThrows(IllegalArgumentException.class, () -> new GarminPersonalApiClient(
                HttpClient.newHttpClient(),
                URI.create("http://example.com/token"),
                Duration.ofSeconds(1),
                Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private GarminPersonalApiClient client(Duration timeout) {
        return new GarminPersonalApiClient(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/token"),
                timeout,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> handler.handle(exchange));
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static String jwt(long expiration) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"exp\":" + expiration + "}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
