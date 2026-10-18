-- Read-only diagnostics for one imported Explorer Tour.
-- Override from psql with: -v tour_id=123
\if :{?tour_id}
\else
\set tour_id 1
\endif

\set ON_ERROR_STOP on
\pset null '[NULL]'
\pset pager off

\echo
\echo 'Inspecting Explorer Tour ID' :tour_id

\echo
\echo '=== 1. Tour summary ==='
SELECT
    id,
    publication_status,
    published_at,
    tour_date,
    distance_km,
    total_ascent_m,
    total_descent_m,
    lowest_point_m,
    highest_point_m,
    route_geometry IS NOT NULL AS has_route_geometry,
    title,
    mountain_range,
    country_code,
    start_location_name,
    start_latitude,
    start_longitude,
    short_description,
    technical_description,
    duration_min_hours,
    duration_max_hours,
    technical_difficulty,
    physical_difficulty,
    exposure_rating,
    scenic_rating,
    carrying_percentage,
    route_type,
    cover_photo_id,
    youtube_video_id
FROM tour
WHERE id = :'tour_id'::bigint;

\echo
\echo '=== 2. Route geometry diagnostics ==='
SELECT
    id AS tour_id,
    ST_GeometryType(route_geometry) AS geometry_type,
    ST_SRID(route_geometry) AS srid,
    ST_NPoints(route_geometry) AS point_count,
    ST_IsValid(route_geometry) AS is_valid,
    ST_IsEmpty(route_geometry) AS is_empty,
    ST_X(ST_StartPoint(route_geometry)) AS start_longitude,
    ST_Y(ST_StartPoint(route_geometry)) AS start_latitude,
    ST_X(ST_EndPoint(route_geometry)) AS end_longitude,
    ST_Y(ST_EndPoint(route_geometry)) AS end_latitude
FROM tour
WHERE id = :'tour_id'::bigint;

\echo
\echo '=== 3a. Elevation profile summary ==='
SELECT
    COUNT(*) AS sample_count,
    MIN(distance_km) AS minimum_distance_km,
    MAX(distance_km) AS maximum_distance_km,
    MIN(elevation_m) AS minimum_elevation_m,
    MAX(elevation_m) AS maximum_elevation_m
FROM tour_elevation_profile
WHERE tour_id = :'tour_id'::bigint;

\echo
\echo '=== 3b. First 10 elevation profile samples ==='
SELECT
    sample_index,
    distance_km,
    elevation_m
FROM tour_elevation_profile
WHERE tour_id = :'tour_id'::bigint
ORDER BY sample_index ASC
LIMIT 10;

\echo
\echo '=== 3c. Last 10 elevation profile samples (displayed in route order) ==='
SELECT
    sample_index,
    distance_km,
    elevation_m
FROM (
    SELECT
        sample_index,
        distance_km,
        elevation_m
    FROM tour_elevation_profile
    WHERE tour_id = :'tour_id'::bigint
    ORDER BY sample_index DESC
    LIMIT 10
) AS final_samples
ORDER BY sample_index ASC;

\echo
\echo '=== 4. GPX source diagnostics ==='
SELECT
    id AS gpx_track_id,
    tour_id,
    original_filename,
    source_reference
FROM gpx_track
WHERE tour_id = :'tour_id'::bigint;

\echo
\echo '=== 5. Child-data counts ==='
SELECT
    :'tour_id'::bigint AS tour_id,
    (SELECT COUNT(*) FROM gpx_track WHERE tour_id = :'tour_id'::bigint) AS gpx_track_count,
    (SELECT COUNT(*) FROM tour_photo WHERE tour_id = :'tour_id'::bigint) AS photo_count,
    (SELECT COUNT(*) FROM tour_bike_type WHERE tour_id = :'tour_id'::bigint) AS bike_type_count,
    (SELECT COUNT(*) FROM tour_surface_share WHERE tour_id = :'tour_id'::bigint) AS surface_share_count,
    (SELECT COUNT(*) FROM tour_elevation_profile WHERE tour_id = :'tour_id'::bigint) AS elevation_profile_sample_count;

\echo
\echo '=== 6. Consistency checks ==='
WITH selected_tour AS (
    SELECT *
    FROM tour
    WHERE id = :'tour_id'::bigint
),
profile_summary AS (
    SELECT
        COUNT(*) AS sample_count,
        MAX(distance_km) AS final_distance_km
    FROM tour_elevation_profile
    WHERE tour_id = :'tour_id'::bigint
),
gpx_summary AS (
    SELECT COUNT(*) AS track_count
    FROM gpx_track
    WHERE tour_id = :'tour_id'::bigint
)
SELECT
    check_name,
    status,
    details
FROM (
    SELECT
        1 AS display_order,
        'Tour exists' AS check_name,
        CASE WHEN EXISTS (SELECT 1 FROM selected_tour) THEN 'OK' ELSE 'FAIL' END AS status,
        'Expected exactly one Tour row for the requested id.' AS details

    UNION ALL

    SELECT
        2,
        'Route geometry present',
        CASE WHEN (SELECT route_geometry FROM selected_tour) IS NOT NULL THEN 'OK' ELSE 'FAIL' END,
        'Imported Tours are expected to have route_geometry.'

    UNION ALL

    SELECT
        3,
        'Route geometry SRID is 4326',
        CASE WHEN ST_SRID((SELECT route_geometry FROM selected_tour)) = 4326 THEN 'OK' ELSE 'FAIL' END,
        'Actual SRID: ' || COALESCE(ST_SRID((SELECT route_geometry FROM selected_tour))::text, 'NULL')

    UNION ALL

    SELECT
        4,
        'Route geometry is a LineString',
        CASE WHEN ST_GeometryType((SELECT route_geometry FROM selected_tour)) = 'ST_LineString'
            THEN 'OK' ELSE 'FAIL' END,
        'Actual type: ' || COALESCE(ST_GeometryType((SELECT route_geometry FROM selected_tour)), 'NULL')

    UNION ALL

    SELECT
        5,
        'Route geometry is not empty',
        CASE WHEN NOT ST_IsEmpty((SELECT route_geometry FROM selected_tour)) THEN 'OK' ELSE 'FAIL' END,
        'ST_IsEmpty: ' || COALESCE(ST_IsEmpty((SELECT route_geometry FROM selected_tour))::text, 'NULL')

    UNION ALL

    SELECT
        6,
        'Route geometry is valid',
        CASE WHEN ST_IsValid((SELECT route_geometry FROM selected_tour)) THEN 'OK' ELSE 'FAIL' END,
        'ST_IsValid: ' || COALESCE(ST_IsValid((SELECT route_geometry FROM selected_tour))::text, 'NULL')

    UNION ALL

    SELECT
        7,
        'Elevation profile present',
        CASE WHEN (SELECT sample_count FROM profile_summary) > 0 THEN 'OK' ELSE 'FAIL' END,
        'Sample count: ' || (SELECT sample_count FROM profile_summary)::text

    UNION ALL

    SELECT
        8,
        'Profile final distance matches Tour distance',
        CASE
            WHEN (SELECT distance_km FROM selected_tour) IS NULL
                OR (SELECT final_distance_km FROM profile_summary) IS NULL THEN 'FAIL'
            WHEN ABS(
                    (SELECT distance_km FROM selected_tour)
                    - (SELECT final_distance_km FROM profile_summary)
                ) <= 0.05 THEN 'OK'
            ELSE 'FAIL'
        END,
        'Tour distance: '
            || COALESCE((SELECT distance_km FROM selected_tour)::text, 'NULL')
            || ' km; profile final distance: '
            || COALESCE((SELECT final_distance_km FROM profile_summary)::text, 'NULL')
            || ' km; tolerance: 0.05 km'

    UNION ALL

    SELECT
        9,
        'Exactly one GPX track',
        CASE WHEN (SELECT track_count FROM gpx_summary) = 1 THEN 'OK' ELSE 'FAIL' END,
        'GPX track count: ' || (SELECT track_count FROM gpx_summary)::text
) AS checks
ORDER BY display_order;
