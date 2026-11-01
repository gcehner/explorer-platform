ALTER TABLE tour
    ADD COLUMN discovery_point geometry(Point, 4326)
    GENERATED ALWAYS AS (
        CASE
            WHEN route_geometry IS NULL THEN NULL
            ELSE ST_LineInterpolatePoint(route_geometry, 0.5)
        END
    ) STORED;

CREATE INDEX idx_tour_published_discovery_point
    ON tour
    USING GIST (discovery_point)
    WHERE publication_status = 'PUBLISHED';

CREATE INDEX idx_tour_published_order
    ON tour (published_at DESC, id DESC)
    WHERE publication_status = 'PUBLISHED';
