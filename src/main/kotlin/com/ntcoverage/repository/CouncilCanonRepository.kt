package com.ntcoverage.repository

import com.ntcoverage.model.CouncilCanonDTO
import com.ntcoverage.model.CouncilCanons
import com.ntcoverage.model.Councils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CouncilCanonRepository {

    fun findByCouncilSlug(slug: String, page: Int = 1, limit: Int = 50): List<CouncilCanonDTO> = transaction {
        val councilId = Councils.select(Councils.id)
            .where { Councils.slug eq slug }
            .singleOrNull()
            ?.get(Councils.id)
            ?.value
            ?: return@transaction emptyList()

        findByCouncilId(councilId, page, limit)
    }

    fun findByCouncilId(councilId: Int, page: Int = 1, limit: Int = 50): List<CouncilCanonDTO> = transaction {
        CouncilCanons.selectAll()
            .where { CouncilCanons.councilId eq councilId }
            .orderBy(CouncilCanons.canonNumber to SortOrder.ASC)
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map {
                CouncilCanonDTO(
                    id = it[CouncilCanons.id].value,
                    canonNumber = it[CouncilCanons.canonNumber],
                    title = it[CouncilCanons.title],
                    canonText = it[CouncilCanons.canonText],
                    topic = it[CouncilCanons.topic]
                )
            }
    }

    fun countByCouncilId(councilId: Int): Int = transaction {
        CouncilCanons.selectAll()
            .where { CouncilCanons.councilId eq councilId }
            .count()
            .toInt()
    }

    fun insertIfMissing(
        councilId: Int,
        canonNumber: Int,
        title: String?,
        canonText: String,
        topic: String?
    ): Boolean = transaction {
        val exists = CouncilCanons.selectAll().where {
            (CouncilCanons.councilId eq councilId) and (CouncilCanons.canonNumber eq canonNumber)
        }.count() > 0

        if (!exists) {
            CouncilCanons.insert {
                it[CouncilCanons.councilId] = councilId
                it[CouncilCanons.canonNumber] = canonNumber
                it[CouncilCanons.title] = title
                it[CouncilCanons.canonText] = canonText
                it[CouncilCanons.topic] = topic
            }
        }
        true
    }
}
