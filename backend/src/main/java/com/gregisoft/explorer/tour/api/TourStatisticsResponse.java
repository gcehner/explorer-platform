package com.gregisoft.explorer.tour.api;

import java.math.BigDecimal;

public record TourStatisticsResponse(
        BigDecimal distanceKm,
        int totalAscentMeters,
        int totalDescentMeters,
        int lowestPointMeters,
        int highestPointMeters
) {
}
