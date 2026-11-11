package com.gregisoft.explorer.cli.http;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GpxImportMetadata(
        BigDecimal distanceKm,
        int totalAscentMeters,
        int totalDescentMeters,
        int lowestPointMeters,
        int highestPointMeters,
        LocalDate tourDate,
        List<ElevationProfileSample> elevationProfile,
        GeoJsonLineString routeGeometry,
        String sourceProvider,
        String sourceActivityId
) {
    public GpxImportMetadata(
            BigDecimal distanceKm,
            int totalAscentMeters,
            int totalDescentMeters,
            int lowestPointMeters,
            int highestPointMeters,
            LocalDate tourDate,
            List<ElevationProfileSample> elevationProfile,
            GeoJsonLineString routeGeometry
    ) {
        this(distanceKm, totalAscentMeters, totalDescentMeters, lowestPointMeters,
                highestPointMeters, tourDate, elevationProfile, routeGeometry, null, null);
    }
    public record ElevationProfileSample(BigDecimal distanceKm, int elevationMeters) {
    }

    public record GeoJsonLineString(String type, List<List<BigDecimal>> coordinates) {
    }
}
