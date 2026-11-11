package com.gregisoft.explorer.cli.garmin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public final class GarminActivityDownloadClient implements GarminActivityDownloader {

    static final URI API_BASE = URI.create("https://personal.connectapi.garmin.com");
    static final int MAX_DOWNLOAD_BYTES = 64 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(2);

    private final HttpClient httpClient;
    private final URI apiBase;
    private final Duration requestTimeout;

    public GarminActivityDownloadClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                API_BASE, REQUEST_TIMEOUT);
    }

    GarminActivityDownloadClient(HttpClient httpClient, URI apiBase, Duration requestTimeout) {
        this.httpClient = httpClient;
        this.apiBase = requireSafeBase(apiBase);
        this.requestTimeout = requestTimeout;
    }

    @Override
    public byte[] download(String bearerToken, GarminActivityId activityId) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new GarminActivityDownloadException("A Garmin bearer token is required.");
        }
        URI uri = apiBase.resolve("/download-service/files/activity/" + activityId.value());
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/zip, application/x-zip-compressed, application/octet-stream")
                .header("Authorization", "Bearer " + bearerToken)
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw responseFailure(response.statusCode());
                }
                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (contentLength > MAX_DOWNLOAD_BYTES) {
                    throw new GarminActivityDownloadException(
                            "Garmin activity download exceeds the 64 MiB safety limit.");
                }
                byte[] downloaded = readLimited(body, MAX_DOWNLOAD_BYTES);
                if (downloaded.length == 0) {
                    throw new GarminActivityDownloadException(
                            "Garmin returned an empty activity download.");
                }
                return downloaded;
            }
        } catch (GarminActivityDownloadException exception) {
            throw exception;
        } catch (HttpTimeoutException exception) {
            throw new GarminActivityDownloadException("Garmin activity download timed out.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GarminActivityDownloadException("Garmin activity download was interrupted.", exception);
        } catch (IOException exception) {
            throw new GarminActivityDownloadException(
                    "Could not download the Garmin activity. Check the network connection and try again.",
                    exception);
        }
    }

    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw new GarminActivityDownloadException(
                        "Garmin activity download exceeds the 64 MiB safety limit.");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static GarminActivityDownloadException responseFailure(int statusCode) {
        return switch (statusCode) {
            case 401, 403 -> new GarminActivityDownloadException(
                    "Garmin rejected activity access. The API key may be expired or revoked.");
            case 404 -> new GarminActivityDownloadException("Garmin activity was not found.");
            case 429 -> new GarminActivityDownloadException(
                    "Garmin rate-limited the activity download. Try again later.");
            default -> new GarminActivityDownloadException(
                    "Garmin activity download failed with HTTP " + statusCode + ".");
        };
    }

    private static URI requireSafeBase(URI base) {
        if (base == null || !base.isAbsolute() || base.getHost() == null
                || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Garmin API base URL is invalid.");
        }
        if ("https".equalsIgnoreCase(base.getScheme())) {
            return base;
        }
        if ("http".equalsIgnoreCase(base.getScheme()) && isLoopback(base.getHost())) {
            return base;
        }
        throw new IllegalArgumentException("Garmin activity downloads require HTTPS.");
    }

    private static boolean isLoopback(String host) {
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (IOException exception) {
            return false;
        }
    }
}
