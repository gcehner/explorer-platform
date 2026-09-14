package com.gregisoft.explorer.tour.api;

import com.gregisoft.explorer.tour.type.PublicationStatus;

public record GpxImportResponse(
        Long tourId,
        PublicationStatus status
) {
}
