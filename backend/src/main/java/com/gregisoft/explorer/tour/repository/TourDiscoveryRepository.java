package com.gregisoft.explorer.tour.repository;

import com.gregisoft.explorer.tour.api.DurationRangeResponse;
import com.gregisoft.explorer.tour.api.StartLocationResponse;
import com.gregisoft.explorer.tour.api.TourSummaryResponse;
import com.gregisoft.explorer.tour.service.TourDiscoveryCriteria;
import com.gregisoft.explorer.tour.type.ExposureRating;
import com.gregisoft.explorer.tour.type.RouteType;
import com.gregisoft.explorer.tour.type.TechnicalDifficulty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class TourDiscoveryRepository {

    private final EntityManager entityManager;

    public TourDiscoveryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<TourSummaryResponse> findPublishedTours(
            TourDiscoveryCriteria criteria,
            OffsetDateTime snapshotAt,
            OffsetDateTime cursorPublishedAt,
            Long cursorId,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    tour.id,
                    tour.title,
                    tour.mountain_range,
                    tour.country_code,
                    tour.short_description,
                    tour.start_location_name,
                    tour.start_latitude,
                    tour.start_longitude,
                    tour.distance_km,
                    tour.total_ascent_m,
                    tour.duration_min_hours,
                    tour.duration_max_hours,
                    tour.technical_difficulty,
                    tour.physical_difficulty,
                    tour.exposure_rating,
                    tour.carrying_percentage,
                    tour.route_type,
                    tour.published_at
                FROM tour
                WHERE tour.publication_status = 'PUBLISHED'
                  AND tour.published_at <= :snapshotAt
                  AND tour.discovery_point && ST_MakeEnvelope(
                        CAST(:west AS double precision),
                        CAST(:south AS double precision),
                        CAST(:east AS double precision),
                        CAST(:north AS double precision),
                        4326
                  )
                  AND ST_Covers(
                        ST_MakeEnvelope(
                            CAST(:west AS double precision),
                            CAST(:south AS double precision),
                            CAST(:east AS double precision),
                            CAST(:north AS double precision),
                            4326
                        ),
                        tour.discovery_point
                  )
                """);

        if (criteria.countryCode() != null) {
            sql.append(" AND tour.country_code = :countryCode");
        }
        List<TechnicalDifficulty> difficulties = criteria.technicalDifficulties().stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
        if (!difficulties.isEmpty()) {
            List<String> placeholders = new ArrayList<>();
            for (int index = 0; index < difficulties.size(); index++) {
                placeholders.add(":technicalDifficulty" + index);
            }
            sql.append(" AND tour.technical_difficulty IN (")
                    .append(String.join(", ", placeholders))
                    .append(")");
        }
        appendMinimum(sql, criteria.distanceMinKm(), "tour.distance_km", "distanceMinKm");
        appendMaximum(sql, criteria.distanceMaxKm(), "tour.distance_km", "distanceMaxKm");
        appendMinimum(sql, criteria.ascentMinMeters(), "tour.total_ascent_m", "ascentMinMeters");
        appendMaximum(sql, criteria.ascentMaxMeters(), "tour.total_ascent_m", "ascentMaxMeters");
        if (cursorPublishedAt != null) {
            sql.append("""
                     AND (
                            tour.published_at < :cursorPublishedAt
                            OR (tour.published_at = :cursorPublishedAt AND tour.id < :cursorId)
                     )
                    """);
        }
        sql.append(" ORDER BY tour.published_at DESC, tour.id DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        TourDiscoveryCriteria.BoundingBox bbox = criteria.boundingBox();
        query.setParameter("west", bbox.west());
        query.setParameter("south", bbox.south());
        query.setParameter("east", bbox.east());
        query.setParameter("north", bbox.north());
        query.setParameter("snapshotAt", snapshotAt);
        if (criteria.countryCode() != null) {
            query.setParameter("countryCode", criteria.countryCode());
        }
        for (int index = 0; index < difficulties.size(); index++) {
            query.setParameter("technicalDifficulty" + index, difficulties.get(index).name());
        }
        setParameter(query, "distanceMinKm", criteria.distanceMinKm());
        setParameter(query, "distanceMaxKm", criteria.distanceMaxKm());
        setParameter(query, "ascentMinMeters", criteria.ascentMinMeters());
        setParameter(query, "ascentMaxMeters", criteria.ascentMaxMeters());
        if (cursorPublishedAt != null) {
            query.setParameter("cursorPublishedAt", cursorPublishedAt);
            query.setParameter("cursorId", cursorId);
        }
        query.setMaxResults(limit);

        return rows(query).stream().map(TourDiscoveryRepository::toResponse).toList();
    }

    private static void appendMinimum(
            StringBuilder sql,
            Object value,
            String column,
            String parameter
    ) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" >= :").append(parameter);
        }
    }

    private static void appendMaximum(
            StringBuilder sql,
            Object value,
            String column,
            String parameter
    ) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" <= :").append(parameter);
        }
    }

    private static void setParameter(Query query, String name, Object value) {
        if (value != null) {
            query.setParameter(name, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(Query query) {
        return query.getResultList();
    }

    private static TourSummaryResponse toResponse(Object[] row) {
        StartLocationResponse startLocation = row[5] == null ? null : new StartLocationResponse(
                (String) row[5],
                (BigDecimal) row[6],
                (BigDecimal) row[7]
        );
        DurationRangeResponse duration = row[10] == null ? null : new DurationRangeResponse(
                (BigDecimal) row[10],
                (BigDecimal) row[11]
        );
        return new TourSummaryResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                startLocation,
                (BigDecimal) row[8],
                number(row[9]),
                duration,
                enumValue(TechnicalDifficulty.class, row[12]),
                number(row[13]),
                enumValue(ExposureRating.class, row[14]),
                number(row[15]),
                enumValue(RouteType.class, row[16]),
                offsetDateTime(row[17])
        );
    }

    private static Integer number(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, Object value) {
        return value == null ? null : Enum.valueOf(type, value.toString());
    }

    private static OffsetDateTime offsetDateTime(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new IllegalStateException("Unsupported published_at value: " + value);
    }
}
