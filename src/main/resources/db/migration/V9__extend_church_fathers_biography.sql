ALTER TABLE church_fathers
  ADD COLUMN manner_of_death VARCHAR(200),
  ADD COLUMN biography_original TEXT,
  ADD COLUMN biography_summary TEXT,
  ADD COLUMN biography_summary_reviewed BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE church_father_translations
  ADD COLUMN manner_of_death VARCHAR(200),
  ADD COLUMN biography_original TEXT,
  ADD COLUMN biography_summary TEXT;
