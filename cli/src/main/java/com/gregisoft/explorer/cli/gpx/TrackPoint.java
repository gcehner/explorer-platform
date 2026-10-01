package com.gregisoft.explorer.cli.gpx;

import java.time.Instant;

public record TrackPoint(
        double latitude,
        double longitude,
        Double elevationMeters,
        Instant timestamp
) {
    public TrackPoint {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new GpxProcessingException("Track latitude must be between -90 and 90.");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new GpxProcessingException("Track longitude must be between -180 and 180.");
        }
        if (elevationMeters != null && !Double.isFinite(elevationMeters)) {
            throw new GpxProcessingException("Track elevation must be finite.");
        }
    }
}
