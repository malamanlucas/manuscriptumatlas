package com.ntcoverage.repository

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.BibleVerseTokens
import com.ntcoverage.model.BibleVerses
import com.ntcoverage.model.BibleVerseTokenDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class BibleTokenRepository {

    private val db get() = BibleDatabaseConfig.database

    fun upsertTokens(verseId: Int, versionId: Int, tokens: List<BibleVerseTokenDTO>) = transaction(db) {
        for (t in tokens) {
            val existing = BibleVerseTokens.selectAll()
                .where {
                    (BibleVerseTokens.verseId eq verseId) and
                    (BibleVerseTokens.versionId eq versionId) and
                    (BibleVerseTokens.position eq t.position)
                }
                .firstOrNull()
            if (existing == null) {
                BibleVerseTokens.insert {
                    it[BibleVerseTokens.verseId] = verseId
                    it[BibleVerseTokens.versionId] = versionId
                    it[BibleVerseTokens.position] = t.position
                    it[BibleVerseTokens.token] = t.token
                    it[BibleVerseTokens.tokenRaw] = t.tokenRaw
                    it[BibleVerseTokens.lemma] = t.lemma
                    it[BibleVerseTokens.isContraction] = t.isContraction
                    it[BibleVerseTokens.contractionParts] = t.contractionParts
                    it[BibleVerseTokens.isEnclitic] = t.isEnclitic
                    it[BibleVerseTokens.encliticParts] = t.encliticParts
                }
            }
        }
    }

    fun getTokensForVerse(verseId: Int, versionId: Int): List<BibleVerseTokenDTO> = transaction(db) {
        BibleVerseTokens.selectAll()
            .where {
                (BibleVerseTokens.verseId eq verseId) and
                (BibleVerseTokens.versionId eq versionId)
            }
            .orderBy(BibleVerseTokens.position to SortOrder.ASC)
            .map { it.toDTO() }
    }

    fun getTokensForChapter(
        bookId: Int, chapter: Int, versionId: Int
    ): Map<Int, List<BibleVerseTokenDTO>> = transaction(db) {
        (BibleVerseTokens innerJoin BibleVerses)
            .selectAll()
            .where {
                (BibleVerses.bookId eq bookId) and
                (BibleVerses.chapter eq chapter) and
                (BibleVerseTokens.versionId eq versionId)
            }
            .orderBy(BibleVerses.verseNumber to SortOrder.ASC, BibleVerseTokens.position to SortOrder.ASC)
            .groupBy { it[BibleVerses.verseNumber] }
            .mapValues { (_, rows) -> rows.map { it.toDTO() } }
    }

    fun hasTokensForVerse(verseId: Int, versionId: Int): Boolean = transaction(db) {
        BibleVerseTokens.selectAll()
            .where {
                (BibleVerseTokens.verseId eq verseId) and
                (BibleVerseTokens.versionId eq versionId)
            }
            .count() > 0
    }

    fun countTokens(versionId: Int): Long = transaction(db) {
        BibleVerseTokens.selectAll()
            .where { BibleVerseTokens.versionId eq versionId }
            .count()
    }

    fun deleteTokensByVersion(versionId: Int): Int = transaction(db) {
        BibleVerseTokens.deleteWhere { BibleVerseTokens.versionId eq versionId }
    }

    fun updateLemma(verseId: Int, versionId: Int, position: Int, lemma: String) = transaction(db) {
        BibleVerseTokens.update({
            (BibleVerseTokens.verseId eq verseId) and
            (BibleVerseTokens.versionId eq versionId) and
            (BibleVerseTokens.position eq position)
        }) {
            it[BibleVerseTokens.lemma] = lemma
        }
    }

    fun getTokensWithoutLemma(versionId: Int, limit: Int = 500): List<Pair<Triple<Int, Int, Int>, String>> = transaction(db) {
        BibleVerseTokens.selectAll()
            .where {
                (BibleVerseTokens.versionId eq versionId) and
                BibleVerseTokens.lemma.isNull() and
                (BibleVerseTokens.isContraction eq false)
            }
            .limit(limit)
            .map { row ->
                Triple(
                    row[BibleVerseTokens.verseId].value,
                    row[BibleVerseTokens.versionId].value,
                    row[BibleVerseTokens.position]
                ) to row[BibleVerseTokens.token]
            }
    }

    private fun ResultRow.toDTO() = BibleVerseTokenDTO(
        position = this[BibleVerseTokens.position],
        token = this[BibleVerseTokens.token],
        tokenRaw = this[BibleVerseTokens.tokenRaw],
        lemma = this[BibleVerseTokens.lemma],
        isContraction = this[BibleVerseTokens.isContraction],
        contractionParts = this[BibleVerseTokens.contractionParts],
        isEnclitic = this[BibleVerseTokens.isEnclitic],
        encliticParts = this[BibleVerseTokens.encliticParts]
    )
}
