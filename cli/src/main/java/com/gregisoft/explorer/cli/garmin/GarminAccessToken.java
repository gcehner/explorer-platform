package com.gregisoft.explorer.cli.garmin;

import java.time.Instant;

public record GarminAccessToken(String value, Instant expiresAt) {

    public GarminAccessToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Access token must not be blank.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Access token expiration is required.");
        }
    }

    @Override
    public String toString() {
        return "GarminAccessToken[expiresAt=" + expiresAt + "]";
    }
}
