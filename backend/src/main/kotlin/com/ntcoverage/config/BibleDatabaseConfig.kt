package com.ntcoverage.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.slf4j.LoggerFactory

object BibleDatabaseConfig {

    private val log = LoggerFactory.getLogger(BibleDatabaseConfig::class.java)
    private lateinit var dataSource: HikariDataSource
    lateinit var database: Database
        private set

    fun init() {
        val url = System.getenv("BIBLE_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/bible_db"
        val user = System.getenv("BIBLE_DATABASE_USER") ?: "postgres"
        val password = System.getenv("BIBLE_DATABASE_PASSWORD") ?: "postgres"
        val maxPoolSize = System.getenv("BIBLE_DATABASE_MAX_POOL_SIZE")?.toIntOrNull() ?: 5

        val config = HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            driverClassName = "org.postgresql.Driver"
            poolName = "BibleHikariPool"
            validate()
        }

        // Save current default database before connecting bible_db
        val previousDefault = TransactionManager.defaultDatabase

        dataSource = HikariDataSource(config)
        database = Database.connect(dataSource)

        // Restore atlas_db as the default database for transaction {} without explicit db
        if (previousDefault != null) {
            TransactionManager.defaultDatabase = previousDefault
        }
        log.info("Bible database connected: $url (pool=$maxPoolSize)")
    }

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
            log.info("Bible database connection closed.")
        }
    }
}
