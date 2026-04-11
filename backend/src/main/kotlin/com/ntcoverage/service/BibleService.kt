package com.ntcoverage.service

import com.ntcoverage.model.*
import com.ntcoverage.repository.BibleBookRepository
import com.ntcoverage.repository.BibleVersionRepository
import com.ntcoverage.repository.BibleVerseRepository
import com.ntcoverage.repository.InterlinearRepository
import com.ntcoverage.repository.LexiconRepository
import org.slf4j.LoggerFactory

class BibleService(
    private val versionRepository: BibleVersionRepository,
    private val bookRepository: BibleBookRepository,
    private val verseRepository: BibleVerseRepository,
    private val referenceParser: BibleReferenceParser,
    private val interlinearRepository: InterlinearRepository,
    private val lexiconRepository: LexiconRepository
) {
    private val log = LoggerFactory.getLogger(BibleService::class.java)

    fun getVersions(testament: String? = null): List<BibleVersionDTO> {
        return versionRepository.findAll(testament?.uppercase())
    }

    fun getBooks(testament: String? = null): List<BibleBookDTO> {
        return bookRepository.findAll(testament?.uppercase())
    }

    fun getChapter(versionCode: String, bookName: String, chapter: Int): BibleChapterResponse {
        val version = versionRepository.findByCode(versionCode)
            ?: throw NoSuchElementException("Version not found: $versionCode")
        val book = resolveBookByName(bookName)

        if (chapter < 1 || chapter > book.totalChapters) {
            throw IllegalArgumentException("Chapter $chapter out of range for ${book.name} (1-${book.totalChapters})")
        }

        val verses = verseRepository.getChapterTexts(version.id, book.id, chapter)

        return BibleChapterResponse(
            version = version.code,
            book = book.name,
            bookId = book.id,
            chapter = chapter,
            totalVerses = verses.size,
            verses = verses
        )
    }

    fun getVerse(versionCode: String, bookName: String, chapter: Int, verse: Int): BibleVerseTextDTO {
        val version = versionRepository.findByCode(versionCode)
            ?: throw NoSuchElementException("Version not found: $versionCode")
        val book = resolveBookByName(bookName)

        return verseRepository.getVerseText(version.id, book.id, chapter, verse)
            ?: throw NoSuchElementException("Verse not found: ${book.name} $chapter:$verse ($versionCode)")
    }

    fun resolveReference(input: String, locale: String = "en"): ResolvedReferenceDTO {
        return referenceParser.parse(input, locale)
            ?: throw IllegalArgumentException("Could not parse reference: $input")
    }

    fun getInterlinearChapter(bookName: String, chapter: Int, alignVersion: String? = null): InterlinearChapterDTO {
        val book = resolveBookByName(bookName)
        val wordsMap = interlinearRepository.getWordsForChapter(book.id, chapter)

        // Fetch alignment data for the requested version
        val alignments = alignVersion?.let {
            interlinearRepository.getAlignmentsForChapter(book.id, chapter, it)
        } ?: emptyMap()

        // Fetch aligned version text for each verse
        val versionTexts = alignVersion?.let { code ->
            versionRepository.findByCode(code)?.let { v ->
                verseRepository.getChapterTexts(v.id, book.id, chapter)
                    .associate { it.verseNumber to it.text }
            }
        } ?: emptyMap()

        val verses = wordsMap.map { (verseNum, words) ->
            val enrichedWords = words.map { word ->
                val alignment = alignments[Pair(verseNum, word.wordPosition)]
                if (alignment != null) word.copy(kjvAlignment = alignment) else word
            }
            InterlinearVerseDTO(
                book = book.name,
                chapter = chapter,
                verseNumber = verseNum,
                words = enrichedWords,
                kjvText = versionTexts[verseNum]
            )
        }.sortedBy { it.verseNumber }

        return InterlinearChapterDTO(
            book = book.name,
            chapter = chapter,
            verses = verses
        )
    }

    fun getInterlinearVerse(bookName: String, chapter: Int, verse: Int, alignVersion: String? = null): InterlinearVerseDTO {
        val book = resolveBookByName(bookName)
        val verseId = verseRepository.getVerseId(book.id, chapter, verse)
            ?: throw NoSuchElementException("Verse not found: ${book.name} $chapter:$verse")
        val words = interlinearRepository.getWordsForVerse(verseId)

        // Fetch alignment data for the requested version
        val alignments = alignVersion?.let {
            interlinearRepository.getAlignmentsForChapter(book.id, chapter, it)
        } ?: emptyMap()
        val enrichedWords = words.map { word ->
            val alignment = alignments[Pair(verse, word.wordPosition)]
            if (alignment != null) word.copy(kjvAlignment = alignment) else word
        }

        // Fetch aligned version text
        val versionText = alignVersion?.let { code ->
            versionRepository.findByCode(code)?.let { v ->
                verseRepository.getVerseText(v.id, book.id, chapter, verse)?.text
            }
        }

        return InterlinearVerseDTO(
            book = book.name,
            chapter = chapter,
            verseNumber = verse,
            words = enrichedWords,
            kjvText = versionText
        )
    }

    fun getLexiconEntry(strongsNumber: String, locale: String = "en"): LexiconEntryDTO {
        return lexiconRepository.findByStrongsNumberWithTranslation(strongsNumber, locale)
            ?: throw NoSuchElementException("Lexicon entry not found: $strongsNumber")
    }

    fun getStrongsConcordance(strongsNumber: String, page: Int = 1, limit: Int = 50): StrongsConcordanceDTO {
        val lexicon = lexiconRepository.findByStrongsNumber(strongsNumber)
        val total = interlinearRepository.countStrongsOccurrences(strongsNumber)
        val occurrences = interlinearRepository.getStrongsConcordance(strongsNumber, page, limit)

        return StrongsConcordanceDTO(
            strongsNumber = strongsNumber.uppercase(),
            lexiconEntry = lexicon,
            totalOccurrences = total.toInt(),
            occurrences = occurrences,
            page = page,
            limit = limit
        )
    }

    fun compareChapter(bookName: String, chapter: Int, versionCodes: List<String>): BibleCompareResponse {
        val book = resolveBookByName(bookName)
        val versions = versionCodes.mapNotNull { versionRepository.findByCode(it) }
        if (versions.isEmpty()) throw IllegalArgumentException("No valid versions provided")

        val rows = verseRepository.compareChapter(book.id, chapter, versions.map { it.id })

        return BibleCompareResponse(
            book = book.name,
            chapter = chapter,
            versions = versions.map { it.code },
            verses = rows
        )
    }

    fun compareVerse(bookName: String, chapter: Int, verse: Int, versionCodes: List<String>): BibleCompareResponse {
        val response = compareChapter(bookName, chapter, versionCodes)
        val filtered = response.verses.filter { it.verseNumber == verse }
        return response.copy(verseNumber = verse, verses = filtered)
    }

    fun searchText(
        query: String, versionCode: String? = null, testament: String? = null,
        bookName: String? = null, locale: String = "en", page: Int = 1, limit: Int = 20
    ): BibleSearchResponse {
        // Check if query is a reference
        val resolved = referenceParser.parse(query, locale)
        if (resolved != null) {
            return BibleSearchResponse(
                query = query,
                totalResults = 0,
                results = emptyList(),
                page = 1,
                limit = limit,
                isReference = true,
                resolvedReference = resolved
            )
        }

        val versionId = versionCode?.let { versionRepository.findByCode(it)?.id }
        val bookId = bookName?.let { resolveBookByName(it).id }

        val (results, total) = verseRepository.searchText(
            query = query,
            versionId = versionId,
            bookId = bookId,
            testament = testament,
            page = page,
            limit = limit
        )

        return BibleSearchResponse(
            query = query,
            totalResults = total,
            results = results,
            page = page,
            limit = limit
        )
    }

    private fun resolveBookByName(name: String): BibleBookDTO {
        return bookRepository.findByNameCaseInsensitive(name)
            ?: bookRepository.findByAbbreviationAnyLocale(name)
            ?: throw NoSuchElementException("Book not found: $name")
    }
}
