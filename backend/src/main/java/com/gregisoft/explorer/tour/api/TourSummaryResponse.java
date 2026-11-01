package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.type.ExposureRating;
import com.gregisoft.explorer.tour.type.RouteType;
import com.gregisoft.explorer.tour.type.TechnicalDifficulty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TourSummaryResponse(
        Long id,
        String title,
        String mountainRange,
        String countryCode,
        String shortDescription,
        StartLocationResponse startLocation,
        BigDecimal distanceKm,
        Integer totalAscentMeters,
        DurationRangeResponse estimatedDuration,
        TechnicalDifficulty technicalDifficulty,
        Integer physicalDifficulty,
        ExposureRating exposureRating,
        Integer carryingPercentage,
        RouteType routeType,
        OffsetDateTime publishedAt
) {
}
