package com.gregisoft.explorer.cli.gpx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NormalizedGpxData(
        BigDecimal distanceKm,
        int totalAscentMeters,
        int totalDescentMeters,
        int lowestPointMeters,
        int highestPointMeters,
        LocalDate tourDate,
        List<ElevationSample> elevationProfile,
        List<List<BigDecimal>> routeCoordinates
) {
    public NormalizedGpxData {
        elevationProfile = List.copyOf(elevationProfile);
        routeCoordinates = routeCoordinates.stream().map(List::copyOf).toList();
    }

    public record ElevationSample(BigDecimal distanceKm, int elevationMeters) {
    }
}
