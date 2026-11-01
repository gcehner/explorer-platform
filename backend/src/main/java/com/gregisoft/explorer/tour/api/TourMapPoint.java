package com.gregisoft.explorer.tour.api;

import java.util.List;

public record TourMapPoint(
        String type,
        List<Double> coordinates
) {
    public TourMapPoint {
        coordinates = List.copyOf(coordinates);
    }
}
