-- Manuscript sources for extended academic metadata
CREATE TABLE manuscript_sources (
    id                 SERIAL PRIMARY KEY,
    manuscript_id      INTEGER NOT NULL REFERENCES manuscripts(id) ON DELETE CASCADE,
    source_name        VARCHAR(100),
    ntvmr_url         VARCHAR(500),
    historical_notes   TEXT,
    geographic_origin  VARCHAR(200),
    discovery_location VARCHAR(200),
    UNIQUE(manuscript_id)
);

CREATE INDEX idx_ms_manuscript ON manuscript_sources(manuscript_id);

-- Optional columns on manuscripts for quick access
ALTER TABLE manuscripts ADD COLUMN historical_notes TEXT;
ALTER TABLE manuscripts ADD COLUMN geographic_origin VARCHAR(200);
ALTER TABLE manuscripts ADD COLUMN discovery_location VARCHAR(200);
ALTER TABLE manuscripts ADD COLUMN ntvmr_url VARCHAR(500);
