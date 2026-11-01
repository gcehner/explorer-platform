package com.gregisoft.explorer.tour.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@ApplicationScoped
public class TourDiscoveryCursorCodec {

    private static final int CURRENT_VERSION = 1;
    private static final int MAX_CURSOR_LENGTH = 2_048;

    private final ObjectMapper objectMapper;

    public TourDiscoveryCursorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(
            OffsetDateTime snapshotAt,
            OffsetDateTime publishedAt,
            long id,
            String criteriaFingerprint
    ) {
        CursorPayload payload = new CursorPayload(
                CURRENT_VERSION,
                snapshotAt,
                publishedAt,
                id,
                criteriaFingerprint
        );
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode tour discovery cursor.", exception);
        }
    }

    public DecodedCursor decode(String cursor, String expectedCriteriaFingerprint) {
        if (cursor == null || cursor.isBlank() || cursor.length() > MAX_CURSOR_LENGTH) {
            throw invalidCursor();
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            CursorPayload payload = objectMapper.readValue(decoded, CursorPayload.class);
            if (payload.version() != CURRENT_VERSION
                    || payload.snapshotAt() == null
                    || payload.publishedAt() == null
                    || payload.id() <= 0
                    || payload.criteriaFingerprint() == null
                    || payload.publishedAt().isAfter(payload.snapshotAt())) {
                throw invalidCursor();
            }
            if (!payload.criteriaFingerprint().equals(expectedCriteriaFingerprint)) {
                throw new TourDiscoveryException(
                        "The cursor does not match the requested bounding box and filters."
                );
            }
            return new DecodedCursor(
                    payload.snapshotAt(),
                    payload.publishedAt(),
                    payload.id()
            );
        } catch (IllegalArgumentException | IOException exception) {
            throw invalidCursor();
        }
    }

    public String fingerprint(TourDiscoveryCriteria criteria) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(criteria.canonicalForm()
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private static TourDiscoveryException invalidCursor() {
        return new TourDiscoveryException("Invalid tour discovery cursor.");
    }

    private record CursorPayload(
            int version,
            OffsetDateTime snapshotAt,
            OffsetDateTime publishedAt,
            long id,
            String criteriaFingerprint
    ) {
    }

    public record DecodedCursor(
            OffsetDateTime snapshotAt,
            OffsetDateTime publishedAt,
            long id
    ) {
    }
}
