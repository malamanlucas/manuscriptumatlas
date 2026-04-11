CREATE TABLE ingestion_metadata (
    id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    status VARCHAR(20) NOT NULL DEFAULT 'idle',
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    duration_ms BIGINT,
    manuscripts_ingested INT DEFAULT 0,
    verses_linked INT DEFAULT 0,
    error_message TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

INSERT INTO ingestion_metadata (id, status) VALUES (1, 'idle');
