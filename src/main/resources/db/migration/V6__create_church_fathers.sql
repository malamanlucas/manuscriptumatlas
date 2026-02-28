CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE church_fathers (
    id                SERIAL PRIMARY KEY,
    display_name      VARCHAR(200) NOT NULL,
    normalized_name   VARCHAR(200) NOT NULL UNIQUE,
    century_min       INT NOT NULL,
    century_max       INT NOT NULL,
    short_description TEXT,
    primary_location  VARCHAR(200),
    tradition         VARCHAR(20) NOT NULL CHECK (tradition IN ('greek','latin','syriac','coptic')),
    source            VARCHAR(100) NOT NULL DEFAULT 'seed',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_church_fathers_century ON church_fathers (century_min, century_max);
CREATE INDEX idx_church_fathers_normalized_name ON church_fathers (normalized_name);
CREATE INDEX idx_church_fathers_tradition ON church_fathers (tradition);
CREATE INDEX idx_church_fathers_name_trgm ON church_fathers USING gin (display_name gin_trgm_ops);
