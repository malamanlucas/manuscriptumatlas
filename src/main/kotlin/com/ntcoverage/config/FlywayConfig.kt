package com.ntcoverage.config

import com.ntcoverage.model.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.SchemaUtils
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
            }
        } catch (e: Exception) {
            log.warn("Flyway migration failed (${e.message}). Falling back to Exposed SchemaUtils...")
            createTablesViaExposed()
        }
    }

    private fun createTablesViaExposed() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Books, Verses, Manuscripts, ManuscriptVerses, CoverageByCentury
            )
            log.info("Tables created/verified via Exposed SchemaUtils.")
        }
    }
}
