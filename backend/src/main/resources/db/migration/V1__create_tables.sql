CREATE TABLE books (
    id             SERIAL PRIMARY KEY,
    name           VARCHAR(50)  NOT NULL UNIQUE,
    abbreviation   VARCHAR(10)  NOT NULL UNIQUE,
    total_chapters INTEGER      NOT NULL,
    total_verses   INTEGER      NOT NULL,
    book_order     INTEGER      NOT NULL UNIQUE
);

CREATE TABLE verses (
    id      SERIAL  PRIMARY KEY,
    book_id INTEGER NOT NULL REFERENCES books(id),
    chapter INTEGER NOT NULL,
    verse   INTEGER NOT NULL,
    UNIQUE(book_id, chapter, verse)
);

CREATE INDEX idx_verses_book ON verses(book_id);
CREATE INDEX idx_verses_book_chapter ON verses(book_id, chapter);

CREATE TABLE manuscripts (
    id                SERIAL       PRIMARY KEY,
    ga_id             VARCHAR(20)  NOT NULL UNIQUE,
    name              VARCHAR(200),
    century_min       INTEGER      NOT NULL,
    century_max       INTEGER      NOT NULL,
    effective_century INTEGER      NOT NULL,
    manuscript_type   VARCHAR(20),
    CHECK (century_min >= 1 AND century_max <= 10),
    CHECK (effective_century = century_min)
);

CREATE INDEX idx_manuscripts_century ON manuscripts(effective_century);

CREATE TABLE manuscript_verses (
    id            SERIAL  PRIMARY KEY,
    manuscript_id INTEGER NOT NULL REFERENCES manuscripts(id),
    verse_id      INTEGER NOT NULL REFERENCES verses(id),
    UNIQUE(manuscript_id, verse_id)
);

CREATE INDEX idx_mv_manuscript ON manuscript_verses(manuscript_id);
CREATE INDEX idx_mv_verse ON manuscript_verses(verse_id);

CREATE TABLE coverage_by_century (
    id               SERIAL       PRIMARY KEY,
    century          INTEGER      NOT NULL,
    book_id          INTEGER      NOT NULL REFERENCES books(id),
    covered_verses   INTEGER      NOT NULL,
    total_verses     INTEGER      NOT NULL,
    coverage_percent DECIMAL(5,2) NOT NULL,
    UNIQUE(century, book_id)
);

CREATE INDEX idx_coverage_century ON coverage_by_century(century);
CREATE INDEX idx_coverage_book ON coverage_by_century(book_id);
