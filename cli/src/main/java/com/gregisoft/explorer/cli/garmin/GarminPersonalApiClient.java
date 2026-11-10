package com.gregisoft.explorer.cli.garmin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public final class GarminPersonalApiClient implements GarminAuthenticator {

    static final URI TOKEN_ENDPOINT = URI.create(
            "https://personal.connectapi.garmin.com/"
                    + "personalapitoken-service/personal-api/personal-api-key/token");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_TOKEN_LENGTH = 16_384;

    private final HttpClient httpClient;
    private final URI tokenEndpoint;
    private final Duration requestTimeout;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GarminPersonalApiClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                TOKEN_ENDPOINT, REQUEST_TIMEOUT, Clock.systemUTC());
    }

    GarminPersonalApiClient(
            HttpClient httpClient,
            URI tokenEndpoint,
            Duration requestTimeout,
            Clock clock
    ) {
        this.httpClient = httpClient;
        this.tokenEndpoint = requireSafeEndpoint(tokenEndpoint);
        if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("Request timeout must be positive.");
        }
        this.requestTimeout = requestTimeout;
        this.clock = clock;
    }

    @Override
    public GarminAccessToken authenticate(char[] apiKey) {
        if (apiKey == null || apiKey.length == 0) {
            throw new GarminAuthenticationException("A Garmin Personal API key is required.");
        }
        String authorization = "ApiKey " + new String(apiKey);
        try {
            HttpRequest request = HttpRequest.newBuilder(tokenEndpoint)
                    .timeout(requestTimeout)
                    .header("Accept", "*/*")
                    .header("Authorization", authorization)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw authenticationFailure(response.statusCode());
            }
            return parseToken(response.body());
        } catch (GarminAuthenticationException exception) {
            throw exception;
        } catch (HttpTimeoutException exception) {
            throw new GarminAuthenticationException("The Garmin authentication request timed out.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GarminAuthenticationException("Garmin authentication was interrupted.", exception);
        } catch (IOException exception) {
            throw new GarminAuthenticationException(
                    "Could not reach the Garmin Personal API. Check the network connection and try again.",
                    exception);
        }
    }

    private GarminAccessToken parseToken(String responseBody) {
        String token = responseBody == null ? "" : responseBody.trim();
        if (token.isEmpty() || token.length() > MAX_TOKEN_LENGTH) {
            throw malformedToken();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[1].isEmpty()) {
            throw malformedToken();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = objectMapper.readTree(payload);
            JsonNode expirationClaim = claims == null ? null : claims.get("exp");
            if (expirationClaim == null || !expirationClaim.canConvertToLong()) {
                throw malformedToken();
            }
            Instant expiration = Instant.ofEpochSecond(expirationClaim.longValue());
            if (!expiration.isAfter(clock.instant())) {
                throw new GarminAuthenticationException("Garmin returned an expired access token.");
            }
            return new GarminAccessToken(token, expiration);
        } catch (IllegalArgumentException | IOException exception) {
            throw new GarminAuthenticationException(
                    "Garmin returned a malformed access token.", exception);
        }
    }

    private static GarminAuthenticationException authenticationFailure(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return new GarminAuthenticationException(
                    "Garmin rejected the Personal API key. It may be invalid, expired, or revoked.");
        }
        if (statusCode == 429) {
            return new GarminAuthenticationException(
                    "Garmin temporarily rate-limited authentication. Try again later.");
        }
        return new GarminAuthenticationException(
                "Garmin authentication failed with HTTP " + statusCode + ". Try again later.");
    }

    private static GarminAuthenticationException malformedToken() {
        return new GarminAuthenticationException("Garmin returned a malformed access token.");
    }

    private static URI requireSafeEndpoint(URI endpoint) {
        if (endpoint == null || !endpoint.isAbsolute() || endpoint.getHost() == null
                || endpoint.getUserInfo() != null || endpoint.getFragment() != null) {
            throw new IllegalArgumentException("Garmin token endpoint is invalid.");
        }
        if ("https".equalsIgnoreCase(endpoint.getScheme())) {
            return endpoint;
        }
        if ("http".equalsIgnoreCase(endpoint.getScheme()) && isLoopback(endpoint.getHost())) {
            return endpoint;
        }
        throw new IllegalArgumentException("Garmin requests require HTTPS.");
    }

    private static boolean isLoopback(String host) {
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (IOException exception) {
            return false;
        }
    }
}
