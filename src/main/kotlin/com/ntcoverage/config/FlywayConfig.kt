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
                FatherTextualStatements, ChurchFatherTranslations, FatherStatementTranslations
            )
            log.info("Tables created/verified via Exposed SchemaUtils.")
        }
        applyExtraIndexesAndConstraints()
        ensureIngestionMetadataRow()
    }

    private fun applyExtraIndexesAndConstraints() {
        transaction {
            exec("CREATE EXTENSION IF NOT EXISTS pg_trgm")
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
