package com.gregisoft.explorer.cli.garmin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GarminActivityDownloadClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void downloadsOnlyFromConfiguredGarminBaseWithBearerAuthentication() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        byte[] zip = {1, 2, 3};
        startServer(exchange -> {
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, zip);
        });

        byte[] result = client(Duration.ofSeconds(2)).download(
                "secret-token", GarminActivityId.parse("123456"));

        assertArrayEquals(zip, result);
        assertTrue(path.get().endsWith("/download-service/files/activity/123456"));
        assertTrue(authorization.get().equals("Bearer secret-token"));
    }

    @Test
    void handlesNotFoundRateLimitAndAuthenticationWithoutLeakingToken() throws Exception {
        for (int status : new int[]{401, 404, 429}) {
            startServer(exchange -> respond(exchange, status, "secret-token".getBytes()));
            GarminActivityDownloadException exception = assertThrows(
                    GarminActivityDownloadException.class,
                    () -> client(Duration.ofSeconds(2)).download(
                            "secret-token", GarminActivityId.parse("123")));
            assertFalse(exception.getMessage().contains("secret-token"));
            server.stop(0);
            server = null;
        }
    }

    @Test
    void handlesTimeoutAndOversizedContentLength() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(250);
                respond(exchange, 200, new byte[]{1});
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // Expected when the timed-out client closes the request.
            }
        });
        GarminActivityDownloadException timeout = assertThrows(
                GarminActivityDownloadException.class,
                () -> client(Duration.ofMillis(40)).download(
                        "token", GarminActivityId.parse("123")));
        assertTrue(timeout.getMessage().contains("timed out"));

        server.stop(0);
        startServer(exchange -> {
            exchange.getResponseHeaders().set(
                    "Content-Length", Long.toString(GarminActivityDownloadClient.MAX_DOWNLOAD_BYTES + 1L));
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        GarminActivityDownloadException oversized = assertThrows(
                GarminActivityDownloadException.class,
                () -> client(Duration.ofSeconds(2)).download(
                        "token", GarminActivityId.parse("123")));
        assertTrue(oversized.getMessage().contains("64 MiB"));
    }

    private GarminActivityDownloadClient client(Duration timeout) {
        return new GarminActivityDownloadClient(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                timeout);
    }

    private void startServer(Handler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler::handle);
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
