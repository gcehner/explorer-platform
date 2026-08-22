package com.gregisoft.explorer.tour.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
public class StartLocation {

    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    @Column(name = "start_location_name", length = 200)
    private String name;

    @Column(name = "start_latitude", precision = 9, scale = 7)
    private BigDecimal latitude;

    @Column(name = "start_longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    protected StartLocation() {
        // Required by Jakarta Persistence.
    }

    public StartLocation(String name, BigDecimal latitude, BigDecimal longitude) {
        this.name = requireText(name, "Start location name");
        this.latitude = requireInRange(
                Objects.requireNonNull(latitude, "Latitude is required."),
                MIN_LATITUDE,
                MAX_LATITUDE,
                "Latitude"
        );
        this.longitude = requireInRange(
                Objects.requireNonNull(longitude, "Longitude is required."),
                MIN_LONGITUDE,
                MAX_LONGITUDE,
                "Longitude"
        );
    }

    public String getName() {
        return name;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }

    private static BigDecimal requireInRange(
            BigDecimal value,
            BigDecimal minimum,
            BigDecimal maximum,
            String fieldName
    ) {
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be between " + minimum + " and " + maximum + "."
            );
        }
        return value;
    }
}
