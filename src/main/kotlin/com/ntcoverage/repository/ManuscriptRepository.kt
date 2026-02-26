package com.ntcoverage.repository

import com.ntcoverage.model.Manuscripts
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ManuscriptRepository {

    fun insertIfNotExists(
        gaId: String,
        name: String?,
        centuryMin: Int,
        centuryMax: Int,
        manuscriptType: String?
    ): Int = transaction {
        val existing = Manuscripts.selectAll()
            .where { Manuscripts.gaId eq gaId }
            .singleOrNull()

        if (existing != null) {
            return@transaction existing[Manuscripts.id].value
        }

        Manuscripts.insertIgnore {
            it[Manuscripts.gaId] = gaId
            it[Manuscripts.name] = name
            it[Manuscripts.centuryMin] = centuryMin
            it[Manuscripts.centuryMax] = centuryMax
            it[Manuscripts.effectiveCentury] = centuryMin
            it[Manuscripts.manuscriptType] = manuscriptType
        }

        Manuscripts.selectAll()
            .where { Manuscripts.gaId eq gaId }
            .single()[Manuscripts.id].value
    }
}
