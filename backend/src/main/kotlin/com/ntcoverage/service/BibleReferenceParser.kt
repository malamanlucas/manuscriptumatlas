package com.ntcoverage.service

import com.ntcoverage.model.BibleBookDTO
import com.ntcoverage.model.ResolvedReferenceDTO
import com.ntcoverage.repository.BibleBookRepository
import org.slf4j.LoggerFactory

class BibleReferenceParser(
    private val bookRepository: BibleBookRepository
) {
    private val log = LoggerFactory.getLogger(BibleReferenceParser::class.java)

    // Matches: "Mt 1:1", "Mt 1.1", "1Co 13:4", "Gn 1.1-5", "Genesis 1", "1 Samuel 3:10"
    // Accepts both : and . as chapter:verse separator
    private val referencePattern = Regex(
        """^(\d?\s?[A-Za-zÀ-ú]+(?:\s+[A-Za-zÀ-ú]+)*)\s+(\d+)(?:[.:]+(\d+)(?:-(\d+))?)?$"""
    )

    fun parse(input: String, locale: String = "en"): ResolvedReferenceDTO? {
        val trimmed = input.trim()
        val match = referencePattern.matchEntire(trimmed) ?: return null

        val bookPart = match.groupValues[1].trim()
        val chapter = match.groupValues[2].toIntOrNull() ?: return null
        val verseStart = match.groupValues[3].toIntOrNull()
        val verseEnd = match.groupValues[4].toIntOrNull()

        val book = resolveBook(bookPart, locale) ?: return null

        return ResolvedReferenceDTO(
            book = book.name,
            bookId = book.id,
            chapter = chapter,
            verseStart = verseStart,
            verseEnd = verseEnd ?: verseStart,
            isChapterOnly = verseStart == null
        )
    }

    fun isReference(input: String): Boolean {
        return referencePattern.matchEntire(input.trim()) != null
    }

    private fun resolveBook(input: String, locale: String): BibleBookDTO? {
        // Try exact name match first
        bookRepository.findByNameCaseInsensitive(input)?.let { return it }

        // Try abbreviation in specified locale
        bookRepository.findByAbbreviation(input, locale)?.let { return it }

        // Try abbreviation in any locale
        bookRepository.findByAbbreviationAnyLocale(input)?.let { return it }

        // Try with normalized input (remove spaces between number and name)
        val normalized = input.replace(Regex("""^(\d)\s+"""), "$1")
        if (normalized != input) {
            bookRepository.findByAbbreviationAnyLocale(normalized)?.let { return it }
        }

        log.debug("Could not resolve book reference: '{}'", input)
        return null
    }
}
