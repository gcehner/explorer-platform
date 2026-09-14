package com.gregisoft.explorer.tour.api;

import java.math.BigDecimal;
import java.util.List;

public record GeoJsonLineStringRequest(
        String type,
        List<List<BigDecimal>> coordinates
) {
}
