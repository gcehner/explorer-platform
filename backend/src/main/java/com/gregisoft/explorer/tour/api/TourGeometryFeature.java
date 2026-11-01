package com.gregisoft.explorer.tour.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public record TourGeometryFeature(
        String type,
        JsonNode geometry,
        Map<String, Long> properties
) {
    public TourGeometryFeature {
        properties = Map.copyOf(properties);
    }
}
