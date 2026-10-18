package com.gregisoft.explorer.tour.api;

import java.math.BigDecimal;

public record DurationRangeResponse(
        BigDecimal minimumHours,
        BigDecimal maximumHours
) {
}
