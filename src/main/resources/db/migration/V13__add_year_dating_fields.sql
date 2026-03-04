-- V13: Add year-level dating fields to manuscripts and church_fathers
-- Enables precise historical dating beyond century granularity.
-- Sources: NTVMR (origEarly/origLate) for manuscripts, OpenAI enrichment for both domains.

ALTER TABLE manuscripts
    ADD COLUMN IF NOT EXISTS year_min INTEGER,
    ADD COLUMN IF NOT EXISTS year_max INTEGER,
    ADD COLUMN IF NOT EXISTS year_best INTEGER,
    ADD COLUMN IF NOT EXISTS dating_source VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dating_reference TEXT,
    ADD COLUMN IF NOT EXISTS dating_confidence VARCHAR(10);

ALTER TABLE church_fathers
    ADD COLUMN IF NOT EXISTS year_min INTEGER,
    ADD COLUMN IF NOT EXISTS year_max INTEGER,
    ADD COLUMN IF NOT EXISTS year_best INTEGER,
    ADD COLUMN IF NOT EXISTS dating_source VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dating_reference TEXT,
    ADD COLUMN IF NOT EXISTS dating_confidence VARCHAR(10);

CREATE INDEX IF NOT EXISTS idx_manuscripts_year_best ON manuscripts(year_best);
CREATE INDEX IF NOT EXISTS idx_church_fathers_year_best ON church_fathers(year_best);

CREATE INDEX IF NOT EXISTS idx_manuscripts_year_range ON manuscripts(year_min, year_max);
CREATE INDEX IF NOT EXISTS idx_church_fathers_year_range ON church_fathers(year_min, year_max);
