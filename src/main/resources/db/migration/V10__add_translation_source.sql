ALTER TABLE church_father_translations
  ADD COLUMN translation_source VARCHAR(20) NOT NULL DEFAULT 'seed';
