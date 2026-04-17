package com.ntcoverage.service

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.BibleBooks
import com.ntcoverage.model.BibleLayer4CoverageDTO
import com.ntcoverage.model.BibleLayer4VerseCoverageDTO
import com.ntcoverage.model.BibleVerseTokens
import com.ntcoverage.model.BibleVerses
import com.ntcoverage.model.BibleVersions
import com.ntcoverage.model.WordAlignments
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class BibleLayer4CoverageService {

    private val db get() = BibleDatabaseConfig.database

    fun getCoverage(book: String, chapter: Int): BibleLayer4CoverageDTO = transaction(db) {
        val bookRow = BibleBooks.selectAll()
            .where { BibleBooks.name eq book }
            .firstOrNull() ?: return@transaction BibleLayer4CoverageDTO(book, chapter, 0, emptyList())
        val bookId = bookRow[BibleBooks.id].value

        val verseRows = BibleVerses.selectAll()
            .where { (BibleVerses.bookId eq bookId) and (BibleVerses.chapter eq chapter) }
            .orderBy(BibleVerses.verseNumber to SortOrder.ASC)
            .map { it[BibleVerses.id].value to it[BibleVerses.verseNumber] }

        if (verseRows.isEmpty()) {
            return@transaction BibleLayer4CoverageDTO(book, chapter, 0, emptyList())
        }

        val verseIds = verseRows.map { it.first }
        val verseIdToNumber = verseRows.toMap()

        val versionByCode = BibleVersions.selectAll()
            .where { BibleVersions.code inList listOf("ARC69", "KJV") }
            .associate { it[BibleVersions.code] to it[BibleVersions.id].value }
        val arc69Id = versionByCode["ARC69"]
        val kjvId = versionByCode["KJV"]

        val tokenVerses: Map<Int, Set<Int>> = if (arc69Id != null || kjvId != null) {
            BibleVerseTokens.select(BibleVerseTokens.verseId, BibleVerseTokens.versionId)
                .where { (BibleVerseTokens.verseId inList verseIds) }
                .withDistinct()
                .groupBy({ it[BibleVerseTokens.versionId].value }, { it[BibleVerseTokens.verseId].value })
                .mapValues { it.value.toSet() }
        } else emptyMap()

        val alignmentVersesByCode: Map<String, Set<Int>> = WordAlignments
            .select(WordAlignments.verseId, WordAlignments.versionCode)
            .where { WordAlignments.verseId inList verseIds }
            .withDistinct()
            .groupBy({ it[WordAlignments.versionCode] }, { it[WordAlignments.verseId].value })
            .mapValues { it.value.toSet() }

        val enrichedVerses: Set<Int> = WordAlignments
            .select(WordAlignments.verseId)
            .where {
                (WordAlignments.verseId inList verseIds) and
                (WordAlignments.versionCode eq "ARC69") and
                WordAlignments.contextualSense.isNotNull()
            }
            .withDistinct()
            .map { it[WordAlignments.verseId].value }
            .toSet()

        val tokensArc69 = arc69Id?.let { tokenVerses[it] } ?: emptySet()
        val tokensKjv = kjvId?.let { tokenVerses[it] } ?: emptySet()
        val alignedKjv = alignmentVersesByCode["KJV"] ?: emptySet()
        val alignedArc69 = alignmentVersesByCode["ARC69"] ?: emptySet()

        val coverageList = verseRows.map { (verseId, verseNumber) ->
            BibleLayer4VerseCoverageDTO(
                verse = verseNumber,
                tokenizeArc69 = verseId in tokensArc69,
                tokenizeKjv = verseId in tokensKjv,
                alignKjv = verseId in alignedKjv,
                alignArc69 = verseId in alignedArc69,
                enrichSemanticsArc69 = verseId in enrichedVerses
            )
        }

        BibleLayer4CoverageDTO(
            book = book,
            chapter = chapter,
            totalVerses = coverageList.size,
            verses = coverageList
        )
    }
}
