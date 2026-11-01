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
        appendCriteriaPredicates(sql, criteria);
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
        bindCriteriaParameters(query, criteria);
        query.setParameter("snapshotAt", snapshotAt);
        if (cursorPublishedAt != null) {
            query.setParameter("cursorPublishedAt", cursorPublishedAt);
            query.setParameter("cursorId", cursorId);
        }
        query.setMaxResults(limit);

        return rows(query).stream().map(TourDiscoveryRepository::toResponse).toList();
    }

    public long countPublishedTourPointsUpTo(
            TourDiscoveryCriteria criteria,
            int maximumCount
    ) {
        StringBuilder eligible = eligibleToursSql("SELECT 1 FROM tour");
        appendCriteriaPredicates(eligible, criteria);
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM (")
                .append(eligible)
                .append(" LIMIT :countLimit) eligible_limited");
        Query query = entityManager.createNativeQuery(sql.toString());
        bindCriteriaParameters(query, criteria);
        query.setParameter("countLimit", maximumCount + 1);
        return ((Number) query.getSingleResult()).longValue();
    }

    public List<MapTourPoint> findPublishedTourPoints(TourDiscoveryCriteria criteria) {
        StringBuilder sql = eligibleToursSql("""
                SELECT
                    tour.id,
                    tour.title,
                    tour.technical_difficulty,
                    ST_X(tour.discovery_point),
                    ST_Y(tour.discovery_point)
                FROM tour
                """);
        appendCriteriaPredicates(sql, criteria);
        sql.append(" ORDER BY tour.id");
        Query query = entityManager.createNativeQuery(sql.toString());
        bindCriteriaParameters(query, criteria);
        return rows(query).stream().map(row -> new MapTourPoint(
                ((Number) row[0]).longValue(),
                (String) row[1],
                enumValue(TechnicalDifficulty.class, row[2]),
                doubleValue(row[3]),
                doubleValue(row[4])
        )).toList();
    }

    public List<GeometryProjection> findPublishedTourGeometries(
            TourDiscoveryCriteria criteria,
            double clipWest,
            double clipSouth,
            double clipEast,
            double clipNorth,
            double simplificationToleranceMeters,
            int maximumTours,
            int maximumVertices
    ) {
        StringBuilder sql = eligibleToursSql("""
                WITH selected_raw AS (
                    SELECT tour.id, tour.route_geometry
                    FROM tour
                """);
        appendCriteriaPredicates(sql, criteria);
        sql.append("""
                    LIMIT :tourLimit
                ), selected AS (
                    SELECT
                        id,
                        route_geometry,
                        COUNT(*) OVER () AS selected_tour_count
                    FROM selected_raw
                ), clipped AS MATERIALIZED (
                    SELECT
                        id,
                        selected_tour_count,
                        ST_CollectionExtract(
                            ST_ClipByBox2D(
                                route_geometry,
                                ST_MakeEnvelope(
                                    CAST(:clipWest AS double precision),
                                    CAST(:clipSouth AS double precision),
                                    CAST(:clipEast AS double precision),
                                    CAST(:clipNorth AS double precision),
                                    4326
                                )
                            ),
                            2
                        ) AS clipped_geometry
                    FROM selected
                ), simplified AS MATERIALIZED (
                    SELECT
                        id,
                        selected_tour_count,
                        ST_SimplifyPreserveTopology(
                            ST_Transform(clipped_geometry, 3857),
                            CAST(:toleranceMeters AS double precision)
                        ) AS render_geometry
                    FROM clipped
                    WHERE NOT ST_IsEmpty(clipped_geometry)
                ), renderable AS (
                    SELECT
                        id,
                        selected_tour_count,
                        render_geometry,
                        ST_NPoints(render_geometry) AS vertex_count
                    FROM simplified
                    WHERE NOT ST_IsEmpty(render_geometry)
                      AND GeometryType(render_geometry) IN ('LINESTRING', 'MULTILINESTRING')
                      AND ST_NPoints(render_geometry) >= 2
                      AND ST_Length(render_geometry) > 0
                ), budgeted AS (
                    SELECT
                        id,
                        selected_tour_count,
                        render_geometry,
                        vertex_count,
                        SUM(vertex_count) OVER () AS total_vertex_count
                    FROM renderable
                )
                SELECT
                    id,
                    selected_tour_count,
                    vertex_count,
                    total_vertex_count,
                    CASE
                        WHEN total_vertex_count <= :maximumVertices
                        THEN ST_AsGeoJSON(ST_Transform(render_geometry, 4326), 7, 0)
                        ELSE NULL
                    END
                FROM budgeted
                ORDER BY id
                """);

        Query query = entityManager.createNativeQuery(sql.toString());
        bindCriteriaParameters(query, criteria);
        query.setParameter("clipWest", clipWest);
        query.setParameter("clipSouth", clipSouth);
        query.setParameter("clipEast", clipEast);
        query.setParameter("clipNorth", clipNorth);
        query.setParameter("toleranceMeters", simplificationToleranceMeters);
        query.setParameter("tourLimit", maximumTours + 1);
        query.setParameter("maximumVertices", maximumVertices);
        return rows(query).stream().map(row -> new GeometryProjection(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).intValue(),
                ((Number) row[2]).intValue(),
                ((Number) row[3]).intValue(),
                (String) row[4]
        )).toList();
    }

    public long countMapCellsUpTo(
            TourDiscoveryCriteria criteria,
            double cellSizeMeters,
            int maximumCount
    ) {
        StringBuilder sql = mapCellsCte(criteria);
        sql.append(" SELECT COUNT(*) FROM (SELECT grid_x, grid_y FROM cells "
                + "GROUP BY grid_x, grid_y LIMIT :countLimit) grouped_cells");
        Query query = entityManager.createNativeQuery(sql.toString());
        bindCriteriaParameters(query, criteria);
        query.setParameter("cellSizeMeters", cellSizeMeters);
        query.setParameter("countLimit", maximumCount + 1);
        return ((Number) query.getSingleResult()).longValue();
    }

    public List<MapCell> findMapCells(TourDiscoveryCriteria criteria, double cellSizeMeters) {
        StringBuilder sql = mapCellsCte(criteria);
        sql.append("""
                 SELECT
                    grid_x,
                    grid_y,
                    COUNT(*),
                    AVG(ST_X(discovery_point)),
                    AVG(ST_Y(discovery_point)),
                    MIN(ST_X(discovery_point)),
                    MIN(ST_Y(discovery_point)),
                    MAX(ST_X(discovery_point)),
                    MAX(ST_Y(discovery_point)),
                    MIN(id),
                    MIN(title),
                    MIN(technical_difficulty)
                 FROM cells
                 GROUP BY grid_x, grid_y
                 ORDER BY grid_x, grid_y
                """);
        Query query = entityManager.createNativeQuery(sql.toString());
        bindCriteriaParameters(query, criteria);
        query.setParameter("cellSizeMeters", cellSizeMeters);
        return rows(query).stream().map(row -> new MapCell(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).intValue(),
                doubleValue(row[3]),
                doubleValue(row[4]),
                doubleValue(row[5]),
                doubleValue(row[6]),
                doubleValue(row[7]),
                doubleValue(row[8]),
                ((Number) row[9]).longValue(),
                (String) row[10],
                enumValue(TechnicalDifficulty.class, row[11])
        )).toList();
    }

    private static StringBuilder eligibleToursSql(String select) {
        return new StringBuilder(select).append("""
                 WHERE tour.publication_status = 'PUBLISHED'
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
    }

    private static StringBuilder mapCellsCte(TourDiscoveryCriteria criteria) {
        StringBuilder sql = eligibleToursSql("""
                WITH eligible AS (
                    SELECT
                        tour.id,
                        tour.title,
                        tour.technical_difficulty,
                        tour.discovery_point,
                        ST_Transform(tour.discovery_point, 3857) AS mercator_point
                    FROM tour
                """);
        appendCriteriaPredicates(sql, criteria);
        sql.append("""
                ), cells AS (
                    SELECT
                        id,
                        title,
                        technical_difficulty,
                        discovery_point,
                        CAST(FLOOR(ST_X(mercator_point)
                                / CAST(:cellSizeMeters AS double precision)) AS bigint) AS grid_x,
                        CAST(FLOOR(ST_Y(mercator_point)
                                / CAST(:cellSizeMeters AS double precision)) AS bigint) AS grid_y
                    FROM eligible
                )
                """);
        return sql;
    }

    private static void appendCriteriaPredicates(
            StringBuilder sql,
            TourDiscoveryCriteria criteria
    ) {
        if (criteria.countryCode() != null) {
            sql.append(" AND tour.country_code = :countryCode");
        }
        List<TechnicalDifficulty> difficulties = sortedDifficulties(criteria);
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
    }

    private static void bindCriteriaParameters(Query query, TourDiscoveryCriteria criteria) {
        TourDiscoveryCriteria.BoundingBox bbox = criteria.boundingBox();
        query.setParameter("west", bbox.west());
        query.setParameter("south", bbox.south());
        query.setParameter("east", bbox.east());
        query.setParameter("north", bbox.north());
        if (criteria.countryCode() != null) {
            query.setParameter("countryCode", criteria.countryCode());
        }
        List<TechnicalDifficulty> difficulties = sortedDifficulties(criteria);
        for (int index = 0; index < difficulties.size(); index++) {
            query.setParameter("technicalDifficulty" + index, difficulties.get(index).name());
        }
        setParameter(query, "distanceMinKm", criteria.distanceMinKm());
        setParameter(query, "distanceMaxKm", criteria.distanceMaxKm());
        setParameter(query, "ascentMinMeters", criteria.ascentMinMeters());
        setParameter(query, "ascentMaxMeters", criteria.ascentMaxMeters());
    }

    private static List<TechnicalDifficulty> sortedDifficulties(
            TourDiscoveryCriteria criteria
    ) {
        return criteria.technicalDifficulties().stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
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

    private static double doubleValue(Object value) {
        return ((Number) value).doubleValue();
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

    public record MapTourPoint(
            long id,
            String title,
            TechnicalDifficulty technicalDifficulty,
            double longitude,
            double latitude
    ) {
    }

    public record MapCell(
            long gridX,
            long gridY,
            int count,
            double longitude,
            double latitude,
            double west,
            double south,
            double east,
            double north,
            long tourId,
            String title,
            TechnicalDifficulty technicalDifficulty
    ) {
    }

    public record GeometryProjection(
            long tourId,
            int selectedTourCount,
            int vertexCount,
            int totalVertexCount,
            String geoJson
    ) {
    }
}
