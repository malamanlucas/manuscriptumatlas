package com.ntcoverage.config

import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object BibleFlywayConfig {

    private val log = LoggerFactory.getLogger(BibleFlywayConfig::class.java)

    fun migrate() {
        val db = BibleDatabaseConfig.database
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(
                BibleVersions,
                BibleBooks,
                BibleBookAbbreviations,
                BibleChapters,
                BibleVerses,
                BibleVerseTexts,
                InterlinearWords,
                GreekLexicon,
                GreekLexiconTranslations,
                HebrewLexicon,
                HebrewLexiconTranslations,
                WordAlignments,
                BibleVerseTokens,
                BibleLayer4Applications,
                InterlinearGlossAudits
            )
            log.info("Bible tables created/verified via Exposed SchemaUtils.")
        }
        applyColumnUpgrades()
        applyExtraIndexes()
    }

    private fun applyColumnUpgrades() {
        val db = BibleDatabaseConfig.database
        transaction(db) {
            // Upgrade varchar columns to text for hebrew_lexicon (enrichment data exceeds 500 chars)
            exec("""
                DO ${'$'}${'$'}
                BEGIN
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'hebrew_lexicon' AND column_name = 'source_url' AND data_type = 'character varying') THEN
                        ALTER TABLE hebrew_lexicon
                            ALTER COLUMN source_url TYPE text,
                            ALTER COLUMN pronunciation TYPE text,
                            ALTER COLUMN phonetic_spelling TYPE text,
                            ALTER COLUMN kjv_translation TYPE text,
                            ALTER COLUMN nasb_translation TYPE text;
                        RAISE NOTICE 'hebrew_lexicon: varchar columns upgraded to text';
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'greek_lexicon' AND column_name = 'source_url' AND data_type = 'character varying') THEN
                        ALTER TABLE greek_lexicon
                            ALTER COLUMN source_url TYPE text,
                            ALTER COLUMN pronunciation TYPE text,
                            ALTER COLUMN phonetic_spelling TYPE text,
                            ALTER COLUMN kjv_translation TYPE text,
                            ALTER COLUMN nasb_translation TYPE text;
                        RAISE NOTICE 'greek_lexicon: varchar columns upgraded to text';
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'interlinear_words' AND column_name = 'portuguese_gloss' AND data_type = 'character varying') THEN
                        ALTER TABLE interlinear_words
                            ALTER COLUMN portuguese_gloss TYPE text,
                            ALTER COLUMN spanish_gloss TYPE text;
                        RAISE NOTICE 'interlinear_words: gloss columns upgraded to text';
                    END IF;
                END
                ${'$'}${'$'};
            """.trimIndent())
            log.info("Bible column upgrades applied (varchar→text).")
        }
    }

    private fun applyExtraIndexes() {
        val db = BibleDatabaseConfig.database
        transaction(db) {
            exec("CREATE EXTENSION IF NOT EXISTS pg_trgm")
            exec("CREATE EXTENSION IF NOT EXISTS unaccent")
            exec("""
                DO ${'$'}${'$'}
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_bba_abbrev_locale') THEN
                        CREATE INDEX idx_bba_abbrev_locale ON bible_book_abbreviations(LOWER(abbreviation), locale);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_bible_verses_book_chapter') THEN
                        CREATE INDEX idx_bible_verses_book_chapter ON bible_verses(book_id, chapter);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_bvt_version_verse') THEN
                        CREATE UNIQUE INDEX idx_bvt_version_verse ON bible_verse_texts(version_id, verse_id);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_iw_verse_pos') THEN
                        CREATE INDEX idx_iw_verse_pos ON interlinear_words(verse_id, word_position);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_iw_strongs') THEN
                        CREATE INDEX idx_iw_strongs ON interlinear_words(strongs_number);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_iw_lemma') THEN
                        CREATE INDEX idx_iw_lemma ON interlinear_words(lemma);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_gl_strongs') THEN
                        CREATE UNIQUE INDEX idx_gl_strongs ON greek_lexicon(strongs_number);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_hl_strongs') THEN
                        CREATE UNIQUE INDEX idx_hl_strongs ON hebrew_lexicon(strongs_number);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_hlt_lexicon_locale') THEN
                        CREATE UNIQUE INDEX idx_hlt_lexicon_locale ON hebrew_lexicon_translations(lexicon_id, locale);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_wa_verse_version') THEN
                        CREATE INDEX idx_wa_verse_version ON word_alignments(verse_id, version_code);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_bvtk_verse_version') THEN
                        CREATE INDEX idx_bvtk_verse_version ON bible_verse_tokens(verse_id, version_id);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_bvtk_contraction') THEN
                        CREATE INDEX idx_bvtk_contraction ON bible_verse_tokens(is_contraction) WHERE is_contraction = true;
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_iga_word_judged') THEN
                        CREATE INDEX idx_iga_word_judged ON interlinear_gloss_audits(word_id, judged_at DESC);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_iga_verdict_unresolved') THEN
                        CREATE INDEX idx_iga_verdict_unresolved ON interlinear_gloss_audits(verdict) WHERE resolved_at IS NULL;
                    END IF;
                END
                ${'$'}${'$'};
            """.trimIndent())
            log.info("Bible extra indexes applied.")
        }
    }
}
