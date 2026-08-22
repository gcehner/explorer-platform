package com.gregisoft.explorer.tour.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class TourStatistics {

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "total_ascent_m")
    private int totalAscentMeters;

    @Column(name = "total_descent_m")
    private int totalDescentMeters;

    @Column(name = "lowest_point_m")
    private int lowestPointMeters;

    @Column(name = "highest_point_m")
    private int highestPointMeters;

    protected TourStatistics() {
        // Required by Jakarta Persistence.
    }

    public TourStatistics(
            BigDecimal distanceKm,
            int totalAscentMeters,
            int totalDescentMeters,
            int lowestPointMeters,
            int highestPointMeters
    ) {
        BigDecimal distance = Objects.requireNonNull(distanceKm, "Distance is required.");
        if (distance.signum() < 0) {
            throw new IllegalArgumentException("Distance must not be negative.");
        }
        if (totalAscentMeters < 0 || totalDescentMeters < 0) {
            throw new IllegalArgumentException("Total ascent and descent must not be negative.");
        }
        if (highestPointMeters < lowestPointMeters) {
            throw new IllegalArgumentException("Highest point must not be below lowest point.");
        }

        this.distanceKm = distance.setScale(2, RoundingMode.UNNECESSARY);
        this.totalAscentMeters = totalAscentMeters;
        this.totalDescentMeters = totalDescentMeters;
        this.lowestPointMeters = lowestPointMeters;
        this.highestPointMeters = highestPointMeters;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public int getTotalAscentMeters() {
        return totalAscentMeters;
    }

    public int getTotalDescentMeters() {
        return totalDescentMeters;
    }

    public int getLowestPointMeters() {
        return lowestPointMeters;
    }

    public int getHighestPointMeters() {
        return highestPointMeters;
    }
}
