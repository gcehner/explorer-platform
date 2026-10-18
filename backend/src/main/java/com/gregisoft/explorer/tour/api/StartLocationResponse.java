package com.gregisoft.explorer.tour.api;

import java.math.BigDecimal;

public record StartLocationResponse(
        String name,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
