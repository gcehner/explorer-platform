package com.gregisoft.explorer.tour.api;

public record PhotoResponse(
        Long id,
        String imageReference,
        String caption,
        int displayOrder
) {
}
