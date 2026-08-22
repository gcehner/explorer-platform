package com.gregisoft.explorer.tour.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class DurationRange {

    private static final BigDecimal HALF_HOUR = new BigDecimal("0.5");

    @Column(name = "duration_min_hours", precision = 4, scale = 1)
    private BigDecimal minimumHours;

    @Column(name = "duration_max_hours", precision = 4, scale = 1)
    private BigDecimal maximumHours;

    protected DurationRange() {
        // Required by Jakarta Persistence.
    }

    public DurationRange(BigDecimal minimumHours, BigDecimal maximumHours) {
        BigDecimal minimum = validateHours(minimumHours, "Minimum duration");
        BigDecimal maximum = validateHours(maximumHours, "Maximum duration");
        if (maximum.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("Maximum duration must not be below minimum duration.");
        }

        this.minimumHours = minimum;
        this.maximumHours = maximum;
    }

    public BigDecimal getMinimumHours() {
        return minimumHours;
    }

    public BigDecimal getMaximumHours() {
        return maximumHours;
    }

    private static BigDecimal validateHours(BigDecimal value, String fieldName) {
        BigDecimal hours = Objects.requireNonNull(value, fieldName + " is required.");
        if (hours.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }
        if (hours.remainder(HALF_HOUR).signum() != 0) {
            throw new IllegalArgumentException(fieldName + " must use 0.5 hour increments.");
        }
        return hours.setScale(1, RoundingMode.UNNECESSARY);
    }
}
