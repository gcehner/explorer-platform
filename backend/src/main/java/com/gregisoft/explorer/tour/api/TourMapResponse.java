package com.gregisoft.explorer.tour.api;

import java.util.List;

public record TourMapResponse(
        String type,
        TourMapMetadata metadata,
        List<TourMapFeature> features
) {
    public TourMapResponse {
        features = List.copyOf(features);
    }
}
