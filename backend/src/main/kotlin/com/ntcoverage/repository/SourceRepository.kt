package com.ntcoverage.repository

import com.ntcoverage.model.SourceDTO
import com.ntcoverage.model.Sources
import com.ntcoverage.seed.SourceSeedEntry
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class SourceRepository {

    fun findAll(): List<SourceDTO> = transaction {
        Sources.selectAll()
            .orderBy(Sources.baseWeight to SortOrder.DESC, Sources.displayName to SortOrder.ASC)
            .map { it.toDto() }
    }

    fun findByName(name: String): SourceDTO? = transaction {
        Sources.selectAll()
            .where { Sources.name eq name }
            .singleOrNull()
            ?.toDto()
    }

    fun findIdByName(name: String): Int? = transaction {
        Sources.select(Sources.id)
            .where { Sources.name eq name }
            .singleOrNull()
            ?.get(Sources.id)
            ?.value
    }

    fun insertOrUpdate(entry: SourceSeedEntry): Int = transaction {
        val existing = Sources.selectAll().where { Sources.name eq entry.name }.singleOrNull()
        if (existing != null) {
            val id = existing[Sources.id].value
            Sources.update({ Sources.id eq id }) {
                it[displayName] = entry.displayName
                it[sourceLevel] = entry.sourceLevel
                it[baseWeight] = entry.baseWeight
                it[url] = entry.url
                it[description] = entry.description
            }
            return@transaction id
        }

        Sources.insertAndGetId {
            it[name] = entry.name
            it[displayName] = entry.displayName
            it[sourceLevel] = entry.sourceLevel
            it[baseWeight] = entry.baseWeight
            it[url] = entry.url
            it[description] = entry.description
            it[createdAt] = OffsetDateTime.now()
        }.value
    }

    fun updateReliability(sourceId: Int, reliability: Double): Boolean = transaction {
        Sources.update({ Sources.id eq sourceId }) {
            it[reliabilityScore] = reliability
        } > 0
    }

    private fun ResultRow.toDto() = SourceDTO(
        id = this[Sources.id].value,
        name = this[Sources.name],
        displayName = this[Sources.displayName],
        sourceLevel = this[Sources.sourceLevel],
        baseWeight = this[Sources.baseWeight],
        reliabilityScore = this[Sources.reliabilityScore],
        url = this[Sources.url],
        description = this[Sources.description]
    )
}
