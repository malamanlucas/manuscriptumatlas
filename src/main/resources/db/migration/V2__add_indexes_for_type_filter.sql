CREATE INDEX idx_manuscripts_type_century ON manuscripts(manuscript_type, effective_century);

CREATE INDEX idx_mv_verse_manuscript ON manuscript_verses(verse_id, manuscript_id);
