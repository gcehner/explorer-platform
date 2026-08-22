package com.gregisoft.explorer.tour.value;

import com.gregisoft.explorer.tour.type.SurfaceType;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;

@Embeddable
public class SurfaceShare {

    @Enumerated(EnumType.STRING)
    @Column(name = "surface_type", nullable = false, length = 30)
    private SurfaceType surfaceType;

    @Column(name = "percentage", nullable = false)
    private int percentage;

    protected SurfaceShare() {
        // Required by Jakarta Persistence.
    }

    public SurfaceShare(SurfaceType surfaceType, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Surface percentage must be between 0 and 100.");
        }
        this.surfaceType = Objects.requireNonNull(surfaceType, "Surface type is required.");
        this.percentage = percentage;
    }

    public SurfaceType getSurfaceType() {
        return surfaceType;
    }

    public int getPercentage() {
        return percentage;
    }
}
