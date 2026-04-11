package com.ntcoverage.model

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object BibleVersions : IntIdTable("bible_versions") {
    val code = varchar("code", 20).uniqueIndex()
    val name = varchar("name", 100)
    val language = varchar("language", 10)
    val description = text("description").nullable()
    val sourceUrl = varchar("source_url", 500).nullable()
    val isPrimary = bool("is_primary").default(false)
    val testamentScope = varchar("testament_scope", 10).default("FULL") // NT, OT, FULL
    val createdAt = timestampWithTimeZone("created_at")
}

object BibleBooks : IntIdTable("bible_books") {
    val name = varchar("name", 50).uniqueIndex()
    val abbreviation = varchar("abbreviation", 10).uniqueIndex()
    val totalChapters = integer("total_chapters")
    val totalVerses = integer("total_verses")
    val bookOrder = integer("book_order").uniqueIndex()
    val testament = varchar("testament", 2) // NT, OT
}

object BibleBookAbbreviations : IntIdTable("bible_book_abbreviations") {
    val bookId = reference("book_id", BibleBooks)
    val locale = varchar("locale", 5)
    val abbreviation = varchar("abbreviation", 20)

    init {
        uniqueIndex(locale, abbreviation)
    }
}

object BibleChapters : IntIdTable("bible_chapters") {
    val bookId = reference("book_id", BibleBooks)
    val chapterNumber = integer("chapter_number")
    val totalVerses = integer("total_verses")

    init {
        uniqueIndex(bookId, chapterNumber)
    }
}

object BibleVerses : IntIdTable("bible_verses") {
    val bookId = reference("book_id", BibleBooks)
    val chapter = integer("chapter")
    val verseNumber = integer("verse_number")

    init {
        uniqueIndex(bookId, chapter, verseNumber)
    }
}

object BibleVerseTexts : IntIdTable("bible_verse_texts") {
    val versionId = reference("version_id", BibleVersions)
    val verseId = reference("verse_id", BibleVerses)
    val text = text("text")

    init {
        uniqueIndex(versionId, verseId)
    }
}

object InterlinearWords : IntIdTable("interlinear_words") {
    val verseId = reference("verse_id", BibleVerses)
    val wordPosition = short("word_position")
    val originalWord = varchar("original_word", 100)
    val transliteration = varchar("transliteration", 100).nullable()
    val lemma = varchar("lemma", 100)
    val morphology = varchar("morphology", 50).nullable()
    val strongsNumber = varchar("strongs_number", 10).nullable()
    val englishGloss = varchar("english_gloss", 200).nullable()
    val portugueseGloss = text("portuguese_gloss").nullable()
    val spanishGloss = text("spanish_gloss").nullable()
    val language = varchar("language", 10).default("greek") // greek, hebrew

    init {
        uniqueIndex(verseId, wordPosition)
    }
}

object GreekLexicon : IntIdTable("greek_lexicon") {
    val strongsNumber = varchar("strongs_number", 10).uniqueIndex()
    val lemma = varchar("lemma", 100)
    val transliteration = varchar("transliteration", 100).nullable()
    val pronunciation = varchar("pronunciation", 200).nullable()
    val shortDefinition = text("short_definition").nullable()
    val fullDefinition = text("full_definition").nullable()
    val partOfSpeech = varchar("part_of_speech", 50).nullable()
    val usageCount = integer("usage_count").default(0)
    val sourceUrl = varchar("source_url", 500).nullable()
    // Enrichment fields (BibleHub-style)
    val phoneticSpelling = varchar("phonetic_spelling", 200).nullable()
    val kjvTranslation = text("kjv_translation").nullable()
    val kjvUsageCount = integer("kjv_usage_count").nullable()
    val nasbTranslation = text("nasb_translation").nullable()
    val wordOrigin = text("word_origin").nullable()
    val strongsExhaustive = text("strongs_exhaustive").nullable()
    val nasExhaustiveOrigin = text("nas_exhaustive_origin").nullable()
    val nasExhaustiveDefinition = text("nas_exhaustive_definition").nullable()
    val nasExhaustiveTranslation = text("nas_exhaustive_translation").nullable()
}

object GreekLexiconTranslations : IntIdTable("greek_lexicon_translations") {
    val lexiconId = reference("lexicon_id", GreekLexicon)
    val locale = varchar("locale", 5) // pt, es
    val shortDefinition = text("short_definition").nullable()
    val fullDefinition = text("full_definition").nullable()
    val kjvTranslation = text("kjv_translation").nullable()
    val wordOrigin = text("word_origin").nullable()
    val strongsExhaustive = text("strongs_exhaustive").nullable()
    val nasExhaustiveOrigin = text("nas_exhaustive_origin").nullable()
    val nasExhaustiveDefinition = text("nas_exhaustive_definition").nullable()
    val nasExhaustiveTranslation = text("nas_exhaustive_translation").nullable()

    init {
        uniqueIndex(lexiconId, locale)
    }
}

object HebrewLexicon : IntIdTable("hebrew_lexicon") {
    val strongsNumber = varchar("strongs_number", 10).uniqueIndex()
    val lemma = varchar("lemma", 100)
    val transliteration = varchar("transliteration", 100).nullable()
    val pronunciation = varchar("pronunciation", 200).nullable()
    val shortDefinition = text("short_definition").nullable()
    val fullDefinition = text("full_definition").nullable()
    val partOfSpeech = varchar("part_of_speech", 50).nullable()
    val usageCount = integer("usage_count").default(0)
    val sourceUrl = varchar("source_url", 500).nullable()
    val phoneticSpelling = varchar("phonetic_spelling", 200).nullable()
    val kjvTranslation = text("kjv_translation").nullable()
    val kjvUsageCount = integer("kjv_usage_count").nullable()
    val nasbTranslation = text("nasb_translation").nullable()
    val wordOrigin = text("word_origin").nullable()
    val strongsExhaustive = text("strongs_exhaustive").nullable()
    val nasExhaustiveOrigin = text("nas_exhaustive_origin").nullable()
    val nasExhaustiveDefinition = text("nas_exhaustive_definition").nullable()
    val nasExhaustiveTranslation = text("nas_exhaustive_translation").nullable()
}

object HebrewLexiconTranslations : IntIdTable("hebrew_lexicon_translations") {
    val lexiconId = reference("lexicon_id", HebrewLexicon)
    val locale = varchar("locale", 5)
    val shortDefinition = text("short_definition").nullable()
    val fullDefinition = text("full_definition").nullable()
    val kjvTranslation = text("kjv_translation").nullable()
    val wordOrigin = text("word_origin").nullable()
    val strongsExhaustive = text("strongs_exhaustive").nullable()
    val nasExhaustiveOrigin = text("nas_exhaustive_origin").nullable()
    val nasExhaustiveDefinition = text("nas_exhaustive_definition").nullable()
    val nasExhaustiveTranslation = text("nas_exhaustive_translation").nullable()

    init {
        uniqueIndex(lexiconId, locale)
    }
}

object WordAlignments : IntIdTable("word_alignments") {
    val verseId = reference("verse_id", BibleVerses)
    val wordPosition = short("word_position")
    val versionCode = varchar("version_code", 20)
    val kjvIndices = text("kjv_indices").nullable()
    val alignedText = varchar("aligned_text", 500).nullable()
    val isDivergent = bool("is_divergent").default(false)
    val confidence = integer("confidence").default(0)

    init {
        uniqueIndex(verseId, wordPosition, versionCode)
    }
}
