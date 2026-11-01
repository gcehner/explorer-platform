package com.gregisoft.explorer.tour.api;

import java.util.List;

public record TourGeometryResponse(
        String type,
        TourGeometryMetadata metadata,
        List<TourGeometryFeature> features
) {
    public TourGeometryResponse {
        features = List.copyOf(features);
    }
}
