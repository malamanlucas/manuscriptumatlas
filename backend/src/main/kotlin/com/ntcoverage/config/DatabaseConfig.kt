package com.ntcoverage.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection
import javax.sql.DataSource

object DatabaseConfig {

    lateinit var dataSource: HikariDataSource
        private set

    fun init(environment: ApplicationEnvironment) {
        val dbUrl = environment.config.property("database.url").getString()
        val dbUser = environment.config.property("database.user").getString()
        val dbPassword = environment.config.property("database.password").getString()
        val maxPoolSize = environment.config.property("database.maxPoolSize").getString().toInt()

        init(dbUrl, dbUser, dbPassword, maxPoolSize)
    }

    fun init(url: String, user: String, password: String, maxPoolSize: Int = 10) {
        val config = HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            driverClassName = "org.postgresql.Driver"
            validate()
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
    }

    fun getDataSource(): DataSource = dataSource

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}
