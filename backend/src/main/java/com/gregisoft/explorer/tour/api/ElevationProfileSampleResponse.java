package com.gregisoft.explorer.tour.api;

import java.math.BigDecimal;

public record ElevationProfileSampleResponse(
        BigDecimal distanceKm,
        int elevationMeters
) {
}
