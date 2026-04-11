package com.ntcoverage.repository

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class BibleVersionRepository {

    private val db get() = BibleDatabaseConfig.database

    fun findAll(testament: String? = null): List<BibleVersionDTO> = transaction(db) {
        val query = BibleVersions.selectAll()
        if (testament != null) {
            query.andWhere { (BibleVersions.testamentScope eq testament) or (BibleVersions.testamentScope eq "FULL") }
        }
        query.orderBy(BibleVersions.isPrimary to SortOrder.DESC, BibleVersions.code to SortOrder.ASC)
            .map { it.toDTO() }
    }

    fun findByCode(code: String): BibleVersionDTO? = transaction(db) {
        BibleVersions.selectAll().where { BibleVersions.code eq code.uppercase() }
            .firstOrNull()?.toDTO()
    }

    fun upsert(code: String, name: String, language: String, description: String?, isPrimary: Boolean, testamentScope: String): Int = transaction(db) {
        val existing = BibleVersions.selectAll().where { BibleVersions.code eq code }.firstOrNull()
        if (existing != null) {
            existing[BibleVersions.id].value
        } else {
            BibleVersions.insertAndGetId {
                it[BibleVersions.code] = code
                it[BibleVersions.name] = name
                it[BibleVersions.language] = language
                it[BibleVersions.description] = description
                it[BibleVersions.isPrimary] = isPrimary
                it[BibleVersions.testamentScope] = testamentScope
                it[BibleVersions.createdAt] = OffsetDateTime.now()
            }.value
        }
    }

    private fun ResultRow.toDTO() = BibleVersionDTO(
        id = this[BibleVersions.id].value,
        code = this[BibleVersions.code],
        name = this[BibleVersions.name],
        language = this[BibleVersions.language],
        description = this[BibleVersions.description],
        isPrimary = this[BibleVersions.isPrimary],
        testamentScope = this[BibleVersions.testamentScope]
    )
}
