package com.gregisoft.explorer.tour.api;

public record TourGeometryMetadata(
        int zoom,
        double simplificationToleranceMeters,
        int tourCount,
        int vertexCount
) {
}
