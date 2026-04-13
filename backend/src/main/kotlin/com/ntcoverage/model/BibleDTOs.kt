package com.ntcoverage.model

import kotlinx.serialization.Serializable

@Serializable
data class BibleVersionDTO(
    val id: Int,
    val code: String,
    val name: String,
    val language: String,
    val description: String? = null,
    val isPrimary: Boolean = false,
    val testamentScope: String = "FULL"
)

@Serializable
data class BibleBookDTO(
    val id: Int,
    val name: String,
    val abbreviation: String,
    val totalChapters: Int,
    val totalVerses: Int,
    val bookOrder: Int,
    val testament: String,
    val abbreviations: Map<String, List<String>> = emptyMap()
)

@Serializable
data class BibleChapterDTO(
    val bookId: Int,
    val chapterNumber: Int,
    val totalVerses: Int
)

@Serializable
data class BibleVerseTextDTO(
    val verseNumber: Int,
    val text: String
)

@Serializable
data class BibleChapterResponse(
    val version: String,
    val book: String,
    val bookId: Int,
    val chapter: Int,
    val totalVerses: Int,
    val verses: List<BibleVerseTextDTO>
)

@Serializable
data class ResolvedReferenceDTO(
    val book: String,
    val bookId: Int,
    val chapter: Int,
    val verseStart: Int? = null,
    val verseEnd: Int? = null,
    val isChapterOnly: Boolean = false
)

@Serializable
data class WordAlignmentDTO(
    val wordPosition: Int,
    val kjvIndices: List<Int>? = null,
    val alignedText: String? = null,
    val isDivergent: Boolean = false,
    val confidence: Int = 0,
    val tokenPositions: List<Int>? = null,
    val method: String? = null,
    val contextualSense: String? = null,
    val semanticRelation: String? = null
)

@Serializable
data class BibleVerseTokenDTO(
    val position: Int,
    val token: String,
    val tokenRaw: String,
    val lemma: String? = null,
    val isContraction: Boolean = false,
    val contractionParts: String? = null,
    val isEnclitic: Boolean = false,
    val encliticParts: String? = null
)

@Serializable
data class InterlinearWordDTO(
    val wordPosition: Int,
    val originalWord: String,
    val transliteration: String? = null,
    val lemma: String,
    val morphology: String? = null,
    val strongsNumber: String? = null,
    val englishGloss: String? = null,
    val portugueseGloss: String? = null,
    val spanishGloss: String? = null,
    val language: String = "greek",
    val kjvAlignment: WordAlignmentDTO? = null
)

@Serializable
data class InterlinearVerseDTO(
    val book: String,
    val chapter: Int,
    val verseNumber: Int,
    val words: List<InterlinearWordDTO>,
    val kjvText: String? = null
)

@Serializable
data class InterlinearChapterDTO(
    val book: String,
    val chapter: Int,
    val verses: List<InterlinearVerseDTO>
)

@Serializable
data class LexiconEntryDTO(
    val id: Int,
    val strongsNumber: String,
    val lemma: String,
    val transliteration: String? = null,
    val pronunciation: String? = null,
    val shortDefinition: String? = null,
    val fullDefinition: String? = null,
    val partOfSpeech: String? = null,
    val usageCount: Int = 0,
    val language: String, // "greek" or "hebrew"
    val phoneticSpelling: String? = null,
    val kjvTranslation: String? = null,
    val kjvUsageCount: Int? = null,
    val nasbTranslation: String? = null,
    val wordOrigin: String? = null,
    val strongsExhaustive: String? = null,
    val nasExhaustiveOrigin: String? = null,
    val nasExhaustiveDefinition: String? = null,
    val nasExhaustiveTranslation: String? = null
)

@Serializable
data class StrongsOccurrenceDTO(
    val book: String,
    val bookOrder: Int,
    val chapter: Int,
    val verseNumber: Int,
    val originalWord: String,
    val lemma: String,
    val morphology: String? = null
)

@Serializable
data class StrongsConcordanceDTO(
    val strongsNumber: String,
    val lexiconEntry: LexiconEntryDTO? = null,
    val totalOccurrences: Int,
    val occurrences: List<StrongsOccurrenceDTO>,
    val page: Int = 1,
    val limit: Int = 50
)

// ── Compare ──

@Serializable
data class BibleCompareVerseDTO(
    val version: String,
    val text: String
)

@Serializable
data class BibleCompareResponse(
    val book: String,
    val chapter: Int,
    val verseNumber: Int? = null,
    val versions: List<String>,
    val verses: List<BibleCompareRow>
)

@Serializable
data class BibleCompareRow(
    val verseNumber: Int,
    val texts: Map<String, String> // versionCode → text
)

// ── Search ──

@Serializable
data class BibleSearchResultDTO(
    val book: String,
    val chapter: Int,
    val verseNumber: Int,
    val version: String,
    val text: String,
    val snippet: String? = null
)

@Serializable
data class BibleSearchResponse(
    val query: String,
    val totalResults: Int,
    val results: List<BibleSearchResultDTO>,
    val page: Int = 1,
    val limit: Int = 20,
    val isReference: Boolean = false,
    val resolvedReference: ResolvedReferenceDTO? = null
)
