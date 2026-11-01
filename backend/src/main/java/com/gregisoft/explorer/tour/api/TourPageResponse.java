package com.gregisoft.explorer.tour.api;

import java.util.List;

public record TourPageResponse(
        List<TourSummaryResponse> items,
        String nextCursor,
        boolean hasMore
) {
    public TourPageResponse {
        items = List.copyOf(items);
    }
}
