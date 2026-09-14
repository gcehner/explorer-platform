package com.gregisoft.explorer.tour.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GpxImportMetadataRequest(
        BigDecimal distanceKm,
        Integer totalAscentMeters,
        Integer totalDescentMeters,
        Integer lowestPointMeters,
        Integer highestPointMeters,
        LocalDate tourDate,
        List<ElevationProfileSampleRequest> elevationProfile,
        GeoJsonLineStringRequest routeGeometry
) {
}
