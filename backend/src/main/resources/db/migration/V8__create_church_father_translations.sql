-- Translation tables for church fathers i18n
-- Follows the same pattern as book_translations (V5)

CREATE TABLE church_father_translations (
    id               SERIAL PRIMARY KEY,
    father_id        INT NOT NULL REFERENCES church_fathers(id) ON DELETE CASCADE,
    locale           VARCHAR(5) NOT NULL,
    display_name     VARCHAR(200) NOT NULL,
    short_description TEXT,
    primary_location VARCHAR(200),
    UNIQUE (father_id, locale)
);

CREATE INDEX idx_cf_translations_lookup ON church_father_translations(locale, father_id);

CREATE TABLE father_statement_translations (
    id             SERIAL PRIMARY KEY,
    statement_id   INT NOT NULL REFERENCES father_textual_statements(id) ON DELETE CASCADE,
    locale         VARCHAR(5) NOT NULL,
    statement_text TEXT NOT NULL,
    UNIQUE (statement_id, locale)
);

CREATE INDEX idx_stmt_translations_lookup ON father_statement_translations(locale, statement_id);
CREATE INDEX idx_stmt_translations_text_trgm
    ON father_statement_translations USING gin (statement_text gin_trgm_ops);
