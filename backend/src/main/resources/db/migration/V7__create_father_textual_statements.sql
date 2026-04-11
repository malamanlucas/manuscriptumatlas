CREATE TABLE father_textual_statements (
    id SERIAL PRIMARY KEY,
    father_id INT NOT NULL REFERENCES church_fathers(id) ON DELETE CASCADE,
    topic VARCHAR(40) NOT NULL
        CHECK (topic IN (
            'MANUSCRIPTS','AUTOGRAPHS','APOCRYPHA','CANON',
            'TEXTUAL_VARIANTS','TRANSLATION','CORRUPTION','SCRIPTURE_AUTHORITY'
        )),
    statement_text TEXT NOT NULL,
    original_language VARCHAR(20),
    original_text TEXT,
    source_work VARCHAR(200),
    source_reference VARCHAR(200),
    approximate_year INT
        CHECK (approximate_year IS NULL OR approximate_year BETWEEN 30 AND 1000),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_statements_father ON father_textual_statements(father_id);
CREATE INDEX idx_statements_topic ON father_textual_statements(topic);
CREATE INDEX idx_statements_year ON father_textual_statements(approximate_year);
CREATE INDEX idx_statements_topic_year ON father_textual_statements(topic, approximate_year);
CREATE INDEX idx_statements_text_trgm ON father_textual_statements USING gin (statement_text gin_trgm_ops);
