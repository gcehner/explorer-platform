package com.gregisoft.explorer.tour.service;

import com.gregisoft.explorer.tour.type.TechnicalDifficulty;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public record TourDiscoveryCriteria(
        BoundingBox boundingBox,
        String countryCode,
        Set<TechnicalDifficulty> technicalDifficulties,
        BigDecimal distanceMinKm,
        BigDecimal distanceMaxKm,
        Integer ascentMinMeters,
        Integer ascentMaxMeters
) {
    public TourDiscoveryCriteria {
        technicalDifficulties = Set.copyOf(technicalDifficulties);
    }

    public String canonicalForm() {
        String difficulties = technicalDifficulties.stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .map(Enum::name)
                .collect(Collectors.joining(","));
        return String.join("|",
                boundingBox.canonicalForm(),
                value(countryCode),
                difficulties,
                decimal(distanceMinKm),
                decimal(distanceMaxKm),
                value(ascentMinMeters),
                value(ascentMaxMeters)
        );
    }

    private static String decimal(BigDecimal value) {
        if (value == null) {
            return "";
        }
        if (value.signum() == 0) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    public record BoundingBox(
            BigDecimal west,
            BigDecimal south,
            BigDecimal east,
            BigDecimal north
    ) {
        public String canonicalForm() {
            return String.join(",",
                    decimal(west), decimal(south), decimal(east), decimal(north));
        }
    }
}
