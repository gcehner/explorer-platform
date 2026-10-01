package com.gregisoft.explorer.cli.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gregisoft.explorer.cli.gpx.NormalizedGpxData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ExplorerBackendClient {

    private static final String IMPORT_PATH = "/api/management/imports/gpx";
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ExplorerBackendClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    ExplorerBackendClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public GpxImportResponse importGpx(
            URI backendUrl,
            Path gpxFile,
            NormalizedGpxData normalizedData
    ) {
        try {
            String boundary = "explorer-" + UUID.randomUUID();
            byte[] metadata = objectMapper.writeValueAsBytes(toMetadata(normalizedData));
            HttpRequest request = HttpRequest.newBuilder(importUri(backendUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(multipartBody(boundary, metadata, gpxFile))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 201) {
                throw new BackendClientException(errorMessage(response));
            }
            GpxImportResponse result = objectMapper.readValue(response.body(), GpxImportResponse.class);
            if (result.tourId() <= 0 || result.status() == null || result.status().isBlank()) {
                throw new BackendClientException("Backend returned an invalid import response.");
            }
            return result;
        } catch (BackendClientException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BackendClientException("GPX import was interrupted.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new BackendClientException(
                    "Could not import GPX through the Explorer backend: " + exception.getMessage(),
                    exception);
        }
    }

    private GpxImportMetadata toMetadata(NormalizedGpxData data) {
        List<GpxImportMetadata.ElevationProfileSample> profile = data.elevationProfile().stream()
                .map(sample -> new GpxImportMetadata.ElevationProfileSample(
                        sample.distanceKm(), sample.elevationMeters()))
                .toList();
        return new GpxImportMetadata(
                data.distanceKm(),
                data.totalAscentMeters(),
                data.totalDescentMeters(),
                data.lowestPointMeters(),
                data.highestPointMeters(),
                data.tourDate(),
                profile,
                new GpxImportMetadata.GeoJsonLineString("LineString", data.routeCoordinates()));
    }

    private static URI importUri(URI backendUrl) {
        String base = backendUrl.toString();
        return URI.create((base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
                + IMPORT_PATH);
    }

    private static HttpRequest.BodyPublisher multipartBody(
            String boundary, byte[] metadata, Path gpxFile) throws IOException {
        String filename = sanitizeHeaderValue(gpxFile.getFileName().toString());
        List<HttpRequest.BodyPublisher> parts = new ArrayList<>();
        parts.add(bytes("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"metadata\"\r\n"
                + "Content-Type: application/json\r\n\r\n"));
        parts.add(HttpRequest.BodyPublishers.ofByteArray(metadata));
        parts.add(bytes("\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"gpxFile\"; filename=\""
                + filename + "\"\r\n"
                + "Content-Type: application/gpx+xml\r\n\r\n"));
        parts.add(HttpRequest.BodyPublishers.ofFile(gpxFile));
        parts.add(bytes("\r\n--" + boundary + "--\r\n"));
        return HttpRequest.BodyPublishers.concat(parts.toArray(HttpRequest.BodyPublisher[]::new));
    }

    private String errorMessage(HttpResponse<String> response) {
        String detail = null;
        try {
            JsonNode body = objectMapper.readTree(response.body());
            if (body != null && body.hasNonNull("message")) {
                detail = body.get("message").asText();
            }
        } catch (IOException ignored) {
            // Fall back to the status-only error below.
        }
        return "Backend rejected GPX import with HTTP " + response.statusCode()
                + (detail == null || detail.isBlank() ? "." : ": " + detail);
    }

    private static HttpRequest.BodyPublisher bytes(String value) {
        return HttpRequest.BodyPublishers.ofByteArray(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sanitizeHeaderValue(String value) {
        return value.replace("\r", "_").replace("\n", "_").replace("\"", "_");
    }
}
