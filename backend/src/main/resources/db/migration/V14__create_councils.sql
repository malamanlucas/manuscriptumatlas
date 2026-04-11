CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE councils (
    id SERIAL PRIMARY KEY,
    display_name VARCHAR(300) NOT NULL,
    normalized_name VARCHAR(300) NOT NULL UNIQUE,
    slug VARCHAR(350) NOT NULL UNIQUE,
    year INT NOT NULL CHECK (year BETWEEN 30 AND 1200),
    year_end INT CHECK (year_end IS NULL OR year_end BETWEEN 30 AND 1200),
    century INT NOT NULL CHECK (century BETWEEN 1 AND 12),
    council_type VARCHAR(30) NOT NULL
        CHECK (council_type IN ('ECUMENICAL','REGIONAL','LOCAL')),
    location VARCHAR(200),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    short_description TEXT,
    main_topics TEXT,
    key_participants TEXT,
    number_of_participants INT CHECK (number_of_participants IS NULL OR number_of_participants > 0),
    original_text TEXT,
    summary TEXT,
    summary_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    wikipedia_url VARCHAR(500),
    wikidata_id VARCHAR(20),
    source VARCHAR(100) NOT NULL DEFAULT 'seed',
    consensus_confidence DOUBLE PRECISION NOT NULL DEFAULT 0.0
        CHECK (consensus_confidence >= 0.0 AND consensus_confidence <= 1.0),
    data_confidence VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
        CHECK (data_confidence IN ('HIGH','MEDIUM','LOW')),
    source_count INT NOT NULL DEFAULT 1 CHECK (source_count >= 0),
    conflict_resolution TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE council_translations (
    id SERIAL PRIMARY KEY,
    council_id INT NOT NULL REFERENCES councils(id) ON DELETE CASCADE,
    locale VARCHAR(5) NOT NULL,
    display_name VARCHAR(300) NOT NULL,
    short_description TEXT,
    location VARCHAR(200),
    main_topics TEXT,
    summary TEXT,
    translation_source VARCHAR(20) NOT NULL DEFAULT 'seed',
    UNIQUE (council_id, locale)
);

CREATE TABLE council_fathers (
    id SERIAL PRIMARY KEY,
    council_id INT NOT NULL REFERENCES councils(id) ON DELETE CASCADE,
    father_id INT NOT NULL REFERENCES church_fathers(id) ON DELETE CASCADE,
    role VARCHAR(50),
    UNIQUE (council_id, father_id)
);

CREATE TABLE sources (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    source_level VARCHAR(20) NOT NULL
        CHECK (source_level IN ('PRIMARY','ACADEMIC','STRUCTURED','AGGREGATOR')),
    base_weight DOUBLE PRECISION NOT NULL CHECK (base_weight >= 0.0 AND base_weight <= 1.0),
    reliability_score DOUBLE PRECISION CHECK (reliability_score IS NULL OR (reliability_score >= 0.0 AND reliability_score <= 1.0)),
    url VARCHAR(500),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE council_source_claims (
    id SERIAL PRIMARY KEY,
    council_id INT NOT NULL REFERENCES councils(id) ON DELETE CASCADE,
    source_id INT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    claimed_year INT CHECK (claimed_year IS NULL OR claimed_year BETWEEN 30 AND 1200),
    claimed_year_end INT CHECK (claimed_year_end IS NULL OR claimed_year_end BETWEEN 30 AND 1200),
    claimed_location VARCHAR(200),
    claimed_participants INT CHECK (claimed_participants IS NULL OR claimed_participants > 0),
    raw_text TEXT,
    source_page VARCHAR(200),
    extracted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (council_id, source_id)
);

CREATE TABLE council_ingestion_phases (
    id SERIAL PRIMARY KEY,
    phase_name VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'idle'
        CHECK (status IN ('idle','running','success','failed')),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    items_processed INT NOT NULL DEFAULT 0 CHECK (items_processed >= 0),
    items_total INT NOT NULL DEFAULT 0 CHECK (items_total >= 0),
    error_message TEXT,
    last_run_by VARCHAR(100)
);

CREATE INDEX idx_councils_century ON councils (century);
CREATE INDEX idx_councils_year ON councils (year);
CREATE INDEX idx_councils_type ON councils (council_type);
CREATE INDEX idx_councils_slug ON councils (slug);
CREATE INDEX idx_councils_confidence ON councils (consensus_confidence);
CREATE INDEX idx_councils_name_trgm ON councils USING gin (display_name gin_trgm_ops);
CREATE INDEX idx_councils_fts ON councils USING gin (
    to_tsvector('english', coalesce(display_name,'') || ' ' || coalesce(summary,'') || ' ' || coalesce(main_topics,''))
);
CREATE INDEX idx_council_translations_lookup ON council_translations (locale, council_id);
CREATE INDEX idx_source_claims_council ON council_source_claims (council_id);
CREATE INDEX idx_source_claims_source ON council_source_claims (source_id);
CREATE INDEX idx_council_phases_status ON council_ingestion_phases (status);
