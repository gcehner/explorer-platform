CREATE TABLE application_info (
    id          INTEGER PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    version     VARCHAR(20) NOT NULL
);

INSERT INTO application_info (id, name, version)
VALUES (1, 'Explorer Platform', '0.1.0');