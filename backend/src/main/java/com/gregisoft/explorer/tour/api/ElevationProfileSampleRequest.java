package com.gregisoft.explorer.tour.api;

import java.math.BigDecimal;

public record ElevationProfileSampleRequest(
        BigDecimal distanceKm,
        Integer elevationMeters
) {
}
