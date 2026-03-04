CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE heresies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL UNIQUE,
    slug VARCHAR(250) NOT NULL UNIQUE,
    description TEXT,
    century_origin INT CHECK (century_origin IS NULL OR century_origin BETWEEN 1 AND 12),
    year_origin INT CHECK (year_origin IS NULL OR year_origin BETWEEN 30 AND 1200),
    key_figure VARCHAR(200),
    wikipedia_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE heresy_translations (
    id SERIAL PRIMARY KEY,
    heresy_id INT NOT NULL REFERENCES heresies(id) ON DELETE CASCADE,
    locale VARCHAR(5) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    UNIQUE (heresy_id, locale)
);

CREATE TABLE council_heresies (
    id SERIAL PRIMARY KEY,
    council_id INT NOT NULL REFERENCES councils(id) ON DELETE CASCADE,
    heresy_id INT NOT NULL REFERENCES heresies(id) ON DELETE CASCADE,
    action VARCHAR(30) NOT NULL DEFAULT 'condemned'
        CHECK (action IN ('condemned','discussed','affirmed')),
    UNIQUE (council_id, heresy_id)
);

CREATE TABLE council_canons (
    id SERIAL PRIMARY KEY,
    council_id INT NOT NULL REFERENCES councils(id) ON DELETE CASCADE,
    canon_number INT NOT NULL CHECK (canon_number > 0),
    title VARCHAR(500),
    canon_text TEXT NOT NULL,
    topic VARCHAR(100),
    UNIQUE (council_id, canon_number)
);

CREATE INDEX idx_heresies_slug ON heresies (slug);
CREATE INDEX idx_heresies_name_trgm ON heresies USING gin (name gin_trgm_ops);
CREATE INDEX idx_heresy_translations_lookup ON heresy_translations (locale, heresy_id);
CREATE INDEX idx_council_heresies_council ON council_heresies (council_id);
CREATE INDEX idx_council_heresies_heresy ON council_heresies (heresy_id);
CREATE INDEX idx_council_canons_council ON council_canons (council_id);
CREATE INDEX idx_council_canons_topic ON council_canons (topic);
