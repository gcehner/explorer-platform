package com.gregisoft.explorer.tour.api;

public record TourMapMetadata(
        int zoom,
        String mode,
        double cellSizeMeters,
        int aggregationAttempt,
        int featureCount,
        int featureCap
) {
}
