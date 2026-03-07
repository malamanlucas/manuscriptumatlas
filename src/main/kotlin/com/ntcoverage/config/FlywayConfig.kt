package com.ntcoverage.config

import com.ntcoverage.model.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object FlywayConfig {

    private val log = LoggerFactory.getLogger(FlywayConfig::class.java)

    fun migrate(dataSource: DataSource) {
        log.info("Running Flyway migrations...")
        try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateMigrationNaming(false)
                .load()

            val result = flyway.migrate()
            log.info("Flyway applied ${result.migrationsExecuted} migration(s). Schema version: ${result.targetSchemaVersion}")

            if (result.migrationsExecuted == 0) {
                log.info("No Flyway migrations applied. Using Exposed SchemaUtils as fallback...")
                createTablesViaExposed()
            } else {
                ensureIngestionMetadataRow()
            }
        } catch (e: Exception) {
            log.warn("Flyway migration failed (${e.message}). Falling back to Exposed SchemaUtils...")
            createTablesViaExposed()
        }
    }

    private fun createTablesViaExposed() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Books, Verses, Manuscripts, ManuscriptVerses, ManuscriptSources,
                CoverageByCentury, IngestionMetadata, BookTranslations, ChurchFathers,
                FatherTextualStatements, ChurchFatherTranslations, FatherStatementTranslations,
                Councils, CouncilTranslations, CouncilFathers, Heresies, HeresyTranslations,
                CouncilHeresies, CouncilCanons, Sources, CouncilSourceClaims, CouncilIngestionPhases,
                CouncilHereticParticipants, VisitorDailyStats, Users
            )
            log.info("Tables created/verified via Exposed SchemaUtils.")
        }
        createVisitorPartitionedTables()
        applyExtraIndexesAndConstraints()
        ensureIngestionMetadataRow()
    }

    private fun createVisitorPartitionedTables() {
        transaction {
            val vsExists = exec("SELECT 1 FROM pg_class WHERE relname = 'visitor_sessions' AND relkind = 'p'") { it.next() } ?: false
            if (!vsExists) {
                exec("""
                    CREATE TABLE visitor_sessions (
                        id BIGSERIAL, visitor_id VARCHAR(36) NOT NULL, session_id VARCHAR(36) NOT NULL,
                        ip_address VARCHAR(100) NOT NULL, user_agent TEXT NOT NULL,
                        browser_name VARCHAR(50), browser_version VARCHAR(30),
                        os_name VARCHAR(50), os_version VARCHAR(30), device_type VARCHAR(10),
                        screen_width SMALLINT, screen_height SMALLINT,
                        viewport_width SMALLINT, viewport_height SMALLINT,
                        language VARCHAR(10), languages TEXT, timezone VARCHAR(50), platform VARCHAR(50),
                        network_info JSONB, device_memory SMALLINT, hardware_concurrency SMALLINT,
                        color_depth SMALLINT, pixel_ratio NUMERIC(4,2), touch_points SMALLINT,
                        cookie_enabled BOOLEAN, do_not_track BOOLEAN,
                        webgl_renderer VARCHAR(200), webgl_vendor VARCHAR(200),
                        canvas_fingerprint VARCHAR(64), referrer TEXT, page_load_time_ms INTEGER,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        last_activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        PRIMARY KEY (id, created_at)
                    ) PARTITION BY RANGE (created_at)
                """.trimIndent())
                log.info("Created partitioned table visitor_sessions.")
            }

            val pvExists = exec("SELECT 1 FROM pg_class WHERE relname = 'page_views' AND relkind = 'p'") { it.next() } ?: false
            if (!pvExists) {
                exec("""
                    CREATE TABLE page_views (
                        id BIGSERIAL, session_id VARCHAR(36) NOT NULL, visitor_id VARCHAR(36) NOT NULL,
                        path VARCHAR(500) NOT NULL, referrer_path VARCHAR(500),
                        duration_ms INTEGER,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        PRIMARY KEY (id, created_at)
                    ) PARTITION BY RANGE (created_at)
                """.trimIndent())
                log.info("Created partitioned table page_views.")
            }

            exec("ALTER TABLE visitor_sessions ALTER COLUMN ip_address TYPE VARCHAR(100)")

            exec("""
                CREATE OR REPLACE FUNCTION create_monthly_partitions(months_ahead INT DEFAULT 2)
                RETURNS void AS ${'$'}fn${'$'}
                DECLARE
                    start_date DATE; end_date DATE; part_name TEXT; i INT;
                BEGIN
                    FOR i IN 0..months_ahead LOOP
                        start_date := date_trunc('month', CURRENT_DATE + (i || ' months')::INTERVAL);
                        end_date := start_date + INTERVAL '1 month';
                        part_name := 'visitor_sessions_' || to_char(start_date, 'YYYY_MM');
                        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = part_name) THEN
                            EXECUTE format('CREATE TABLE %I PARTITION OF visitor_sessions FOR VALUES FROM (%L) TO (%L)', part_name, start_date, end_date);
                        END IF;
                        part_name := 'page_views_' || to_char(start_date, 'YYYY_MM');
                        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = part_name) THEN
                            EXECUTE format('CREATE TABLE %I PARTITION OF page_views FOR VALUES FROM (%L) TO (%L)', part_name, start_date, end_date);
                        END IF;
                    END LOOP;
                END;
                ${'$'}fn${'$'} LANGUAGE plpgsql
            """.trimIndent())

            exec("SELECT create_monthly_partitions(2)")

            applyVisitorIndexes()
            log.info("Visitor partitioned tables, partitions and indexes ready.")
        }
    }

    private fun applyVisitorIndexes() {
        transaction {
            val indexes = listOf(
                "CREATE INDEX IF NOT EXISTS idx_vs_created_at ON visitor_sessions (created_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_vs_last_activity ON visitor_sessions (last_activity_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_vs_session_id ON visitor_sessions (session_id)",
                "CREATE INDEX IF NOT EXISTS idx_vs_visitor_id ON visitor_sessions (visitor_id, created_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_vs_ip ON visitor_sessions (ip_address, created_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_vs_created_device ON visitor_sessions (created_at DESC, device_type)",
                "CREATE INDEX IF NOT EXISTS idx_vs_created_browser ON visitor_sessions (created_at DESC, browser_name)",
                "CREATE INDEX IF NOT EXISTS idx_vs_created_os ON visitor_sessions (created_at DESC, os_name)",
                "CREATE INDEX IF NOT EXISTS idx_vs_language ON visitor_sessions (language, created_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_vs_timezone ON visitor_sessions (timezone, created_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_pv_created_at ON page_views (created_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_pv_session_id ON page_views (session_id, created_at ASC)",
                "CREATE INDEX IF NOT EXISTS idx_pv_visitor_id ON page_views (visitor_id, created_at DESC)",
                "CREATE INDEX IF NOT EXISTS idx_pv_path_created ON page_views (path, created_at DESC)",
            )
            for (sql in indexes) {
                exec(sql)
            }
        }
    }

    private fun applyExtraIndexesAndConstraints() {
        transaction {
            exec("CREATE EXTENSION IF NOT EXISTS pg_trgm")
            exec("CREATE EXTENSION IF NOT EXISTS unaccent")
            exec("ALTER TABLE IF EXISTS councils DROP CONSTRAINT IF EXISTS councils_normalized_name_unique")
            exec("""
                DO ${'$'}${'$'}
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_statements_father') THEN
                        CREATE INDEX idx_statements_father ON father_textual_statements(father_id);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_statements_topic') THEN
                        CREATE INDEX idx_statements_topic ON father_textual_statements(topic);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_statements_year') THEN
                        CREATE INDEX idx_statements_year ON father_textual_statements(approximate_year);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_statements_topic_year') THEN
                        CREATE INDEX idx_statements_topic_year ON father_textual_statements(topic, approximate_year);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_statements_text_trgm') THEN
                        CREATE INDEX idx_statements_text_trgm ON father_textual_statements USING gin (statement_text gin_trgm_ops);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_cf_translations_lookup') THEN
                        CREATE INDEX idx_cf_translations_lookup ON church_father_translations(locale, father_id);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_stmt_translations_lookup') THEN
                        CREATE INDEX idx_stmt_translations_lookup ON father_statement_translations(locale, statement_id);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_stmt_translations_text_trgm') THEN
                        CREATE INDEX idx_stmt_translations_text_trgm ON father_statement_translations USING gin (statement_text gin_trgm_ops);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_manuscripts_year_best') THEN
                        CREATE INDEX idx_manuscripts_year_best ON manuscripts(year_best);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_church_fathers_year_best') THEN
                        CREATE INDEX idx_church_fathers_year_best ON church_fathers(year_best);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_manuscripts_year_range') THEN
                        CREATE INDEX idx_manuscripts_year_range ON manuscripts(year_min, year_max);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_church_fathers_year_range') THEN
                        CREATE INDEX idx_church_fathers_year_range ON church_fathers(year_min, year_max);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_councils_century') THEN
                        CREATE INDEX idx_councils_century ON councils(century);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_councils_year') THEN
                        CREATE INDEX idx_councils_year ON councils(year);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_councils_type') THEN
                        CREATE INDEX idx_councils_type ON councils(council_type);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_councils_slug') THEN
                        CREATE INDEX idx_councils_slug ON councils(slug);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_councils_confidence') THEN
                        CREATE INDEX idx_councils_confidence ON councils(consensus_confidence);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_councils_name_trgm') THEN
                        CREATE INDEX idx_councils_name_trgm ON councils USING gin (display_name gin_trgm_ops);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_councils_fts') THEN
                        CREATE INDEX idx_councils_fts ON councils USING gin (
                            to_tsvector('english',
                                coalesce(display_name,'') || ' ' ||
                                coalesce(summary,'') || ' ' ||
                                coalesce(main_topics,'')
                            )
                        );
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_council_translations_lookup') THEN
                        CREATE INDEX idx_council_translations_lookup ON council_translations(locale, council_id);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_heresies_slug') THEN
                        CREATE INDEX idx_heresies_slug ON heresies(slug);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_heresies_name_trgm') THEN
                        CREATE INDEX idx_heresies_name_trgm ON heresies USING gin (name gin_trgm_ops);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_source_claims_council') THEN
                        CREATE INDEX idx_source_claims_council ON council_source_claims(council_id);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_source_claims_source') THEN
                        CREATE INDEX idx_source_claims_source ON council_source_claims(source_id);
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_council_phases_status') THEN
                        CREATE INDEX idx_council_phases_status ON council_ingestion_phases(status);
                    END IF;
                END
                ${'$'}${'$'};
            """.trimIndent())
            log.info("Extra indexes and constraints applied for father_textual_statements and translations.")
        }
    }

    private fun ensureIngestionMetadataRow() {
        transaction {
            val exists = IngestionMetadata.selectAll().count() > 0
            if (!exists) {
                IngestionMetadata.insert {
                    it[id] = 1
                    it[status] = "idle"
                }
                log.info("Inserted initial ingestion_metadata row.")
            }
        }
    }
}
