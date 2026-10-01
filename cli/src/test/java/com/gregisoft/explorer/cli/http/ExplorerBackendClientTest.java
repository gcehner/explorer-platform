package com.gregisoft.explorer.cli.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gregisoft.explorer.cli.gpx.NormalizedGpxData;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorerBackendClientTest {

    @TempDir
    Path tempDirectory;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsExpectedMultipartMetadataAndOriginalFileBytes() throws Exception {
        byte[] originalBytes = "<?xml version=\"1.0\"?>\r\n<gpx>untouched ä</gpx>\r\n"
                .getBytes(StandardCharsets.UTF_8);
        Path file = Files.write(tempDirectory.resolve("original track.gpx"), originalBytes);
        AtomicReference<HttpExchangeData> captured = new AtomicReference<>();
        startServer(exchange -> {
            captured.set(new HttpExchangeData(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestBody().readAllBytes()));
            respond(exchange, 201, "{\"tourId\":42,\"status\":\"DRAFT\"}");
        });

        GpxImportResponse result = new ExplorerBackendClient().importGpx(baseUri(), file, normalized());

        assertEquals(42, result.tourId());
        assertEquals("DRAFT", result.status());
        assertEquals("POST", captured.get().method());
        assertEquals("/api/management/imports/gpx", captured.get().uri().getPath());
        assertTrue(captured.get().contentType().startsWith("multipart/form-data; boundary="));

        String boundary = captured.get().contentType().substring(
                captured.get().contentType().indexOf("boundary=") + "boundary=".length());
        byte[] body = captured.get().body();
        String bodyText = new String(body, StandardCharsets.UTF_8);
        assertTrue(bodyText.contains("name=\"metadata\""));
        assertTrue(bodyText.contains("Content-Type: application/json"));
        assertTrue(bodyText.contains("name=\"gpxFile\"; filename=\"original track.gpx\""));
        assertTrue(bodyText.contains("Content-Type: application/gpx+xml"));

        int metadataStart = bodyText.indexOf("\r\n\r\n") + 4;
        int metadataEnd = bodyText.indexOf("\r\n--" + boundary, metadataStart);
        JsonNode metadata = new ObjectMapper().readTree(bodyText.substring(metadataStart, metadataEnd));
        assertEquals("12.34", metadata.get("distanceKm").asText());
        assertEquals("LineString", metadata.at("/routeGeometry/type").asText());
        assertEquals("14.1234567", metadata.at("/routeGeometry/coordinates/0/0").asText());
        assertEquals("46.7654321", metadata.at("/routeGeometry/coordinates/0/1").asText());

        byte[] fileHeader = ("Content-Type: application/gpx+xml\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        int fileStart = indexOf(body, fileHeader, 0) + fileHeader.length;
        byte[] closing = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        int fileEnd = indexOf(body, closing, fileStart);
        assertArrayEquals(originalBytes, java.util.Arrays.copyOfRange(body, fileStart, fileEnd));
    }

    @Test
    void reportsBackendErrorsFor400422And500() throws Exception {
        Path file = Files.writeString(tempDirectory.resolve("track.gpx"), "gpx");
        for (int status : List.of(400, 422, 500)) {
            stopServer();
            server = null;
            startServer(exchange -> {
                exchange.getRequestBody().readAllBytes();
                respond(exchange, status, "{\"message\":\"problem " + status + "\"}");
            });

            BackendClientException exception = assertThrows(
                    BackendClientException.class,
                    () -> new ExplorerBackendClient().importGpx(baseUri(), file, normalized()));

            assertTrue(exception.getMessage().contains("HTTP " + status));
            assertTrue(exception.getMessage().contains("problem " + status));
        }
    }

    @Test
    void reportsUnavailableBackendWithoutLeakingTransportException() throws Exception {
        Path file = Files.writeString(tempDirectory.resolve("track.gpx"), "gpx");

        BackendClientException exception = assertThrows(
                BackendClientException.class,
                () -> new ExplorerBackendClient().importGpx(
                        URI.create("http://127.0.0.1:1"), file, normalized()));

        assertTrue(exception.getMessage().startsWith("Could not import GPX through the Explorer backend:"));
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/management/imports/gpx", exchange -> handler.handle(exchange));
        server.start();
    }

    private URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static int indexOf(byte[] source, byte[] target, int fromIndex) {
        for (int index = fromIndex; index <= source.length - target.length; index++) {
            boolean match = true;
            for (int targetIndex = 0; targetIndex < target.length; targetIndex++) {
                if (source[index + targetIndex] != target[targetIndex]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return index;
            }
        }
        throw new AssertionError("Multipart marker not found");
    }

    private static NormalizedGpxData normalized() {
        return new NormalizedGpxData(
                new BigDecimal("12.34"),
                500,
                490,
                100,
                600,
                LocalDate.of(2025, 7, 12),
                List.of(
                        new NormalizedGpxData.ElevationSample(new BigDecimal("0.000"), 100),
                        new NormalizedGpxData.ElevationSample(new BigDecimal("12.340"), 110)),
                List.of(
                        List.of(new BigDecimal("14.1234567"), new BigDecimal("46.7654321")),
                        List.of(new BigDecimal("14.2234567"), new BigDecimal("46.8654321"))));
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record HttpExchangeData(String method, URI uri, String contentType, byte[] body) {
    }
}
