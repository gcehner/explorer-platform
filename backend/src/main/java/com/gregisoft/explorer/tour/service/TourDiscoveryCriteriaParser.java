package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.type.TechnicalDifficulty;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class TourDiscoveryCriteriaParser {

    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

    private TourDiscoveryCriteriaParser() {
    }

    public static TourDiscoveryCriteria parse(
            String bbox,
            String countryCode,
            String technicalDifficulty,
            String distanceMinKm,
            String distanceMaxKm,
            String ascentMinMeters,
            String ascentMaxMeters
    ) {
        TourDiscoveryCriteria.BoundingBox boundingBox = parseBoundingBox(bbox);
        String normalizedCountryCode = parseCountryCode(countryCode);
        Set<TechnicalDifficulty> technicalDifficulties =
                parseTechnicalDifficulties(technicalDifficulty);
        BigDecimal minimumDistance = parseNonNegativeDecimal(distanceMinKm, "distanceMinKm");
        BigDecimal maximumDistance = parseNonNegativeDecimal(distanceMaxKm, "distanceMaxKm");
        Integer minimumAscent = parseNonNegativeInteger(ascentMinMeters, "ascentMinMeters");
        Integer maximumAscent = parseNonNegativeInteger(ascentMaxMeters, "ascentMaxMeters");

        requireOrdered(minimumDistance, maximumDistance,
                "distanceMinKm must not exceed distanceMaxKm.");
        requireOrdered(minimumAscent, maximumAscent,
                "ascentMinMeters must not exceed ascentMaxMeters.");

        return new TourDiscoveryCriteria(
                boundingBox,
                normalizedCountryCode,
                technicalDifficulties,
                minimumDistance,
                maximumDistance,
                minimumAscent,
                maximumAscent
        );
    }

    private static TourDiscoveryCriteria.BoundingBox parseBoundingBox(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            throw new TourDiscoveryException("The bbox query parameter is required.");
        }
        String[] parts = bbox.split(",", -1);
        if (parts.length != 4) {
            throw new TourDiscoveryException(
                    "Invalid bounding box; expected west,south,east,north."
            );
        }

        BigDecimal west = parseDecimal(parts[0], "bbox west");
        BigDecimal south = parseDecimal(parts[1], "bbox south");
        BigDecimal east = parseDecimal(parts[2], "bbox east");
        BigDecimal north = parseDecimal(parts[3], "bbox north");

        requireRange(west, MIN_LONGITUDE, MAX_LONGITUDE, "Bounding-box longitude");
        requireRange(east, MIN_LONGITUDE, MAX_LONGITUDE, "Bounding-box longitude");
        requireRange(south, MIN_LATITUDE, MAX_LATITUDE, "Bounding-box latitude");
        requireRange(north, MIN_LATITUDE, MAX_LATITUDE, "Bounding-box latitude");
        if (south.compareTo(north) >= 0) {
            throw new TourDiscoveryException(
                    "Invalid bounding box; south must be less than north."
            );
        }
        if (west.compareTo(east) >= 0) {
            throw new TourDiscoveryException(
                    "Invalid bounding box; west must be less than east. "
                            + "Antimeridian-crossing boxes are not supported."
            );
        }
        return new TourDiscoveryCriteria.BoundingBox(west, south, east, north);
    }

    private static String parseCountryCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!COUNTRY_CODE.matcher(normalized).matches()) {
            throw new TourDiscoveryException(
                    "countryCode must be an ISO 3166-1 alpha-2 code."
            );
        }
        return normalized;
    }

    private static Set<TechnicalDifficulty> parseTechnicalDifficulties(String value) {
        if (value == null) {
            return Set.of();
        }
        if (value.isBlank()) {
            throw new TourDiscoveryException("technicalDifficulty must not be empty.");
        }
        EnumSet<TechnicalDifficulty> difficulties = EnumSet.noneOf(TechnicalDifficulty.class);
        for (String part : value.split(",", -1)) {
            String normalized = part.trim().toUpperCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                throw new TourDiscoveryException("technicalDifficulty contains an empty value.");
            }
            try {
                difficulties.add(TechnicalDifficulty.valueOf(normalized));
            } catch (IllegalArgumentException exception) {
                throw new TourDiscoveryException(
                        "Unsupported technicalDifficulty value: " + part.trim() + "."
                );
            }
        }
        return difficulties;
    }

    private static BigDecimal parseNonNegativeDecimal(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        BigDecimal parsed = parseDecimal(value, fieldName);
        if (parsed.signum() < 0) {
            throw new TourDiscoveryException(fieldName + " must not be negative.");
        }
        return parsed;
    }

    private static Integer parseNonNegativeInteger(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0) {
                throw new TourDiscoveryException(fieldName + " must not be negative.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new TourDiscoveryException(fieldName + " must be an integer.");
        }
    }

    private static BigDecimal parseDecimal(String value, String fieldName) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new TourDiscoveryException(fieldName + " must be a finite number.");
        }
    }

    private static void requireRange(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum,
            String fieldName
    ) {
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new TourDiscoveryException(
                    fieldName + " must be between " + minimum + " and " + maximum + "."
            );
        }
    }

    private static <T extends Comparable<T>> void requireOrdered(
            T minimum,
            T maximum,
            String message
    ) {
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new TourDiscoveryException(message);
        }
    }
}
