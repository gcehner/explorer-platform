package com.gregisoft.explorer.tour.api;

import java.util.List;

public record TourLatestResponse(List<TourSummaryResponse> items) {
    public TourLatestResponse {
        items = List.copyOf(items);
    }
}
