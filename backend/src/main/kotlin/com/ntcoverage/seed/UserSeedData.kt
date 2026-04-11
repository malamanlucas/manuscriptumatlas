package com.ntcoverage.seed

import com.ntcoverage.model.UserRole
import com.ntcoverage.model.Users
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.ZoneOffset

object UserSeedData {

    private val log = LoggerFactory.getLogger(UserSeedData::class.java)

    fun seedIfEmpty() {
        transaction {
            if (Users.selectAll().count() > 0L) {
                log.info("Users table already populated — skipping seed.")
                return@transaction
            }

            val now = OffsetDateTime.now(ZoneOffset.UTC)

            Users.insert {
                it[email] = "malamanlucas@gmail.com"
                it[displayName] = "Lucas Malaman"
                it[role] = UserRole.ADMIN
                it[createdAt] = now
            }

            Users.insert {
                it[email] = "malamanalucas@gmail.com"
                it[displayName] = "Lucas (Member)"
                it[role] = UserRole.MEMBER
                it[createdAt] = now
            }

            log.info("Seeded 2 initial users (1 ADMIN, 1 MEMBER).")
        }
    }
}
