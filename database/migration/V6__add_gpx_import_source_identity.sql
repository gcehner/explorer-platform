ALTER TABLE gpx_track
    ADD COLUMN source_provider VARCHAR(50),
    ADD COLUMN source_activity_id VARCHAR(255),

    ADD CONSTRAINT chk_gpx_track_source_identity_complete
        CHECK (
            (source_provider IS NULL AND source_activity_id IS NULL)
            OR
            (source_provider IS NOT NULL
                AND BTRIM(source_provider) <> ''
                AND source_activity_id IS NOT NULL
                AND BTRIM(source_activity_id) <> '')
        ),

    ADD CONSTRAINT chk_gpx_track_source_provider
        CHECK (
            source_provider IS NULL
            OR source_provider IN ('GARMIN_CONNECT')
        ),

    ADD CONSTRAINT uq_gpx_track_source_identity
        UNIQUE (source_provider, source_activity_id);
