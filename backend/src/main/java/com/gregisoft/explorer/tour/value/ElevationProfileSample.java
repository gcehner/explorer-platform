package com.gregisoft.explorer.tour.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class ElevationProfileSample {

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 3)
    private BigDecimal distanceKm;

    @Column(name = "elevation_m", nullable = false)
    private int elevationMeters;

    protected ElevationProfileSample() {
        // Required by Jakarta Persistence.
    }

    public ElevationProfileSample(BigDecimal distanceKm, int elevationMeters) {
        BigDecimal distance = Objects.requireNonNull(distanceKm, "Sample distance is required.");
        if (distance.signum() < 0) {
            throw new IllegalArgumentException("Sample distance must not be negative.");
        }
        this.distanceKm = distance;
        this.elevationMeters = elevationMeters;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public int getElevationMeters() {
        return elevationMeters;
    }
}
