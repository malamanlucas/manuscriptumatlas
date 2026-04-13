package com.ntcoverage.repository

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class InterlinearRepository {

    private val db get() = BibleDatabaseConfig.database

    fun deleteAllWords() = transaction(db) {
        InterlinearWords.deleteAll()
    }

    fun clearTranslatedGlosses(): Int = transaction(db) {
        InterlinearWords.update({ InterlinearWords.portugueseGloss.isNotNull() or InterlinearWords.spanishGloss.isNotNull() }) {
            it[portugueseGloss] = null
            it[spanishGloss] = null
        }
    }

    fun clearCorruptedPortugueseGlosses(): Int = transaction(db) {
        InterlinearWords.update({
            InterlinearWords.portugueseGloss.isNotNull() and
                InterlinearWords.portugueseGloss.like("%\": \"%")
        }) {
            it[portugueseGloss] = null
        }
    }

    fun updateGlosses(wordId: Int, portugueseGloss: String?, spanishGloss: String?) = transaction(db) {
        InterlinearWords.update({ InterlinearWords.id eq wordId }) {
            if (portugueseGloss != null) it[InterlinearWords.portugueseGloss] = portugueseGloss
            if (spanishGloss != null) it[InterlinearWords.spanishGloss] = spanishGloss
        }
    }

    fun getWordsForChapterWithIds(bookId: Int, chapter: Int): List<Pair<Int, InterlinearWordDTO>> = transaction(db) {
        (InterlinearWords innerJoin BibleVerses)
            .selectAll()
            .where { (BibleVerses.bookId eq bookId) and (BibleVerses.chapter eq chapter) }
            .orderBy(BibleVerses.verseNumber to SortOrder.ASC, InterlinearWords.wordPosition to SortOrder.ASC)
            .map { row ->
                val id = row[InterlinearWords.id].value
                val dto = InterlinearWordDTO(
                    wordPosition = row[InterlinearWords.wordPosition].toInt(),
                    originalWord = row[InterlinearWords.originalWord],
                    transliteration = row[InterlinearWords.transliteration],
                    lemma = row[InterlinearWords.lemma],
                    morphology = row[InterlinearWords.morphology],
                    strongsNumber = row[InterlinearWords.strongsNumber],
                    englishGloss = row[InterlinearWords.englishGloss],
                    portugueseGloss = row[InterlinearWords.portugueseGloss],
                    spanishGloss = row[InterlinearWords.spanishGloss],
                    language = row[InterlinearWords.language]
                )
                Pair(id, dto)
            }
    }

    fun upsertWord(
        verseId: Int, wordPosition: Short, originalWord: String,
        transliteration: String?, lemma: String, morphology: String?,
        strongsNumber: String?, englishGloss: String? = null, language: String
    ) = transaction(db) {
        val existing = InterlinearWords.selectAll()
            .where { (InterlinearWords.verseId eq verseId) and (InterlinearWords.wordPosition eq wordPosition) }
            .firstOrNull()
        if (existing == null) {
            InterlinearWords.insert {
                it[InterlinearWords.verseId] = verseId
                it[InterlinearWords.wordPosition] = wordPosition
                it[InterlinearWords.originalWord] = originalWord
                it[InterlinearWords.transliteration] = transliteration
                it[InterlinearWords.lemma] = lemma
                it[InterlinearWords.morphology] = morphology
                it[InterlinearWords.strongsNumber] = strongsNumber
                it[InterlinearWords.englishGloss] = englishGloss
                it[InterlinearWords.language] = language
            }
        }
    }

    fun getWordsForVerse(verseId: Int): List<InterlinearWordDTO> = transaction(db) {
        InterlinearWords.selectAll()
            .where { InterlinearWords.verseId eq verseId }
            .orderBy(InterlinearWords.wordPosition to SortOrder.ASC)
            .map { it.toDTO() }
    }

    fun getWordsForChapter(bookId: Int, chapter: Int): Map<Int, List<InterlinearWordDTO>> = transaction(db) {
        (InterlinearWords innerJoin BibleVerses)
            .selectAll()
            .where { (BibleVerses.bookId eq bookId) and (BibleVerses.chapter eq chapter) }
            .orderBy(BibleVerses.verseNumber to SortOrder.ASC, InterlinearWords.wordPosition to SortOrder.ASC)
            .groupBy { it[BibleVerses.verseNumber] }
            .mapValues { (_, rows) -> rows.map { it.toInterlinearDTO() } }
    }

    fun getStrongsConcordance(strongsNumber: String, page: Int = 1, limit: Int = 50): List<StrongsOccurrenceDTO> = transaction(db) {
        val offset = ((page - 1) * limit).toLong()
        (InterlinearWords innerJoin BibleVerses innerJoin BibleBooks)
            .select(
                BibleBooks.name, BibleBooks.bookOrder,
                BibleVerses.chapter, BibleVerses.verseNumber,
                InterlinearWords.originalWord, InterlinearWords.lemma, InterlinearWords.morphology
            )
            .where { InterlinearWords.strongsNumber eq strongsNumber }
            .orderBy(BibleBooks.bookOrder to SortOrder.ASC, BibleVerses.chapter to SortOrder.ASC, BibleVerses.verseNumber to SortOrder.ASC)
            .limit(limit).offset(offset)
            .map {
                StrongsOccurrenceDTO(
                    book = it[BibleBooks.name],
                    bookOrder = it[BibleBooks.bookOrder],
                    chapter = it[BibleVerses.chapter],
                    verseNumber = it[BibleVerses.verseNumber],
                    originalWord = it[InterlinearWords.originalWord],
                    lemma = it[InterlinearWords.lemma],
                    morphology = it[InterlinearWords.morphology]
                )
            }
    }

    fun countStrongsOccurrences(strongsNumber: String): Long = transaction(db) {
        InterlinearWords.selectAll()
            .where { InterlinearWords.strongsNumber eq strongsNumber }
            .count()
    }

    fun countWords(): Long = transaction(db) {
        InterlinearWords.selectAll().count()
    }

    // ── Word Alignment CRUD ──

    fun upsertAlignment(
        verseId: Int, wordPosition: Short, versionCode: String,
        kjvIndices: String?, alignedText: String?, isDivergent: Boolean,
        confidence: Int = 0,
        tokenPositions: String? = null,
        method: String? = null,
        contextualSense: String? = null,
        semanticRelation: String? = null
    ) = transaction(db) {
        val existing = WordAlignments.selectAll()
            .where {
                (WordAlignments.verseId eq verseId) and
                (WordAlignments.wordPosition eq wordPosition) and
                (WordAlignments.versionCode eq versionCode)
            }
            .firstOrNull()
        if (existing == null) {
            WordAlignments.insert {
                it[WordAlignments.verseId] = verseId
                it[WordAlignments.wordPosition] = wordPosition
                it[WordAlignments.versionCode] = versionCode
                it[WordAlignments.kjvIndices] = kjvIndices
                it[WordAlignments.alignedText] = alignedText
                it[WordAlignments.isDivergent] = isDivergent
                it[WordAlignments.confidence] = confidence
                if (tokenPositions != null) it[WordAlignments.tokenPositions] = tokenPositions
                if (method != null) it[WordAlignments.method] = method
                if (contextualSense != null) it[WordAlignments.contextualSense] = contextualSense
                if (semanticRelation != null) it[WordAlignments.semanticRelation] = semanticRelation
            }
        } else {
            WordAlignments.update({
                (WordAlignments.verseId eq verseId) and
                (WordAlignments.wordPosition eq wordPosition) and
                (WordAlignments.versionCode eq versionCode)
            }) {
                it[WordAlignments.kjvIndices] = kjvIndices
                it[WordAlignments.alignedText] = alignedText
                it[WordAlignments.isDivergent] = isDivergent
                it[WordAlignments.confidence] = confidence
                if (tokenPositions != null) it[WordAlignments.tokenPositions] = tokenPositions
                if (method != null) it[WordAlignments.method] = method
                if (contextualSense != null) it[WordAlignments.contextualSense] = contextualSense
                if (semanticRelation != null) it[WordAlignments.semanticRelation] = semanticRelation
            }
        }
    }

    fun getAlignmentsForChapter(
        bookId: Int, chapter: Int, versionCode: String
    ): Map<Pair<Int, Int>, WordAlignmentDTO> = transaction(db) {
        (WordAlignments innerJoin BibleVerses)
            .selectAll()
            .where {
                (BibleVerses.bookId eq bookId) and
                (BibleVerses.chapter eq chapter) and
                (WordAlignments.versionCode eq versionCode)
            }
            .associate { row ->
                val verseNumber = row[BibleVerses.verseNumber]
                val pos = row[WordAlignments.wordPosition].toInt()
                val indicesJson = row[WordAlignments.kjvIndices]
                val indices = indicesJson?.let { json ->
                    try {
                        json.trim('[', ']').split(",")
                            .filter { it.isNotBlank() }
                            .map { it.trim().toInt() }
                    } catch (_: Exception) { null }
                }
                val tokenPosJson = row[WordAlignments.tokenPositions]
                val tokenPositions = tokenPosJson?.let { json ->
                    try {
                        json.trim('[', ']').split(",")
                            .filter { it.isNotBlank() }
                            .map { it.trim().toInt() }
                    } catch (_: Exception) { null }
                }
                Pair(verseNumber, pos) to WordAlignmentDTO(
                    wordPosition = pos,
                    kjvIndices = indices,
                    alignedText = row[WordAlignments.alignedText],
                    isDivergent = row[WordAlignments.isDivergent],
                    confidence = row[WordAlignments.confidence],
                    tokenPositions = tokenPositions,
                    method = row[WordAlignments.method],
                    contextualSense = row[WordAlignments.contextualSense],
                    semanticRelation = row[WordAlignments.semanticRelation]
                )
            }
    }

    fun hasAlignmentsForVerse(verseId: Int, versionCode: String): Boolean = transaction(db) {
        WordAlignments.selectAll()
            .where { (WordAlignments.verseId eq verseId) and (WordAlignments.versionCode eq versionCode) }
            .count() > 0
    }

    fun deleteAlignmentsByVersion(versionCode: String): Int = transaction(db) {
        WordAlignments.deleteWhere { WordAlignments.versionCode eq versionCode }
    }

    fun countAlignments(versionCode: String): Long = transaction(db) {
        WordAlignments.selectAll()
            .where { WordAlignments.versionCode eq versionCode }
            .count()
    }

    private fun ResultRow.toDTO() = InterlinearWordDTO(
        wordPosition = this[InterlinearWords.wordPosition].toInt(),
        originalWord = this[InterlinearWords.originalWord],
        transliteration = this[InterlinearWords.transliteration],
        lemma = this[InterlinearWords.lemma],
        morphology = this[InterlinearWords.morphology],
        strongsNumber = this[InterlinearWords.strongsNumber],
        englishGloss = this[InterlinearWords.englishGloss],
        portugueseGloss = this[InterlinearWords.portugueseGloss],
        spanishGloss = this[InterlinearWords.spanishGloss],
        language = this[InterlinearWords.language]
    )

    private fun ResultRow.toInterlinearDTO() = InterlinearWordDTO(
        wordPosition = this[InterlinearWords.wordPosition].toInt(),
        originalWord = this[InterlinearWords.originalWord],
        transliteration = this[InterlinearWords.transliteration],
        lemma = this[InterlinearWords.lemma],
        morphology = this[InterlinearWords.morphology],
        strongsNumber = this[InterlinearWords.strongsNumber],
        englishGloss = this[InterlinearWords.englishGloss],
        portugueseGloss = this[InterlinearWords.portugueseGloss],
        spanishGloss = this[InterlinearWords.spanishGloss],
        language = this[InterlinearWords.language]
    )
}
