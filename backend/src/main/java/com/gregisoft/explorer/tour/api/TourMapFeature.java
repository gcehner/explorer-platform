package com.gregisoft.explorer.tour.api;

import java.util.Map;

public record TourMapFeature(
        String type,
        TourMapPoint geometry,
        Map<String, Object> properties
) {
    public TourMapFeature {
        properties = Map.copyOf(properties);
    }
}
