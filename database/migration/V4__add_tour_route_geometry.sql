CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE tour
    ADD COLUMN route_geometry geometry(LineString, 4326),
    ADD CONSTRAINT chk_tour_route_geometry
        CHECK (
            route_geometry IS NULL
            OR
            (NOT ST_IsEmpty(route_geometry)
                AND ST_IsValid(route_geometry)
                AND ST_CoveredBy(
                    route_geometry,
                    ST_MakeEnvelope(-180, -90, 180, 90, 4326)
                ))
        );

CREATE INDEX idx_tour_route_geometry
    ON tour
    USING GIST (route_geometry);
