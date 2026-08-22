package com.gregisoft.explorer.tour.value;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueObjectTest {

    @Test
    void durationMustUseHalfHourIncrements() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DurationRange(new BigDecimal("3.2"), new BigDecimal("4.5"))
        );
    }

    @Test
    void durationMaximumCannotBeBelowMinimum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DurationRange(new BigDecimal("5.0"), new BigDecimal("4.5"))
        );
    }

    @Test
    void latitudeAndLongitudeMustBeInRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StartLocation(
                        "Invalid",
                        new BigDecimal("90.0000001"),
                        BigDecimal.ZERO
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new StartLocation(
                        "Invalid",
                        BigDecimal.ZERO,
                        new BigDecimal("180.0000001")
                )
        );
    }

    @Test
    void statisticsRejectInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TourStatistics(new BigDecimal("-1.00"), 10, 10, 100, 200)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TourStatistics(new BigDecimal("10.00"), 10, 10, 200, 100)
        );
    }
}
