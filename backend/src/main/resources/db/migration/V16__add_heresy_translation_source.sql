-- Coluna translation_source em heresy_translations (seed, machine, reviewed)
-- Permite distinguir traduções curadas (seed), geradas por IA (machine) ou revisadas (reviewed)
ALTER TABLE heresy_translations
ADD COLUMN IF NOT EXISTS translation_source VARCHAR(20) DEFAULT 'seed';
