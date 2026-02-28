package com.ntcoverage.model

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Books : IntIdTable("books") {
    val name = varchar("name", 50).uniqueIndex()
    val abbreviation = varchar("abbreviation", 10).uniqueIndex()
    val totalChapters = integer("total_chapters")
    val totalVerses = integer("total_verses")
    val bookOrder = integer("book_order").uniqueIndex()
}

object Verses : IntIdTable("verses") {
    val bookId = reference("book_id", Books)
    val chapter = integer("chapter")
    val verse = integer("verse")

    init {
        uniqueIndex(bookId, chapter, verse)
    }
}

object Manuscripts : IntIdTable("manuscripts") {
    val gaId = varchar("ga_id", 20).uniqueIndex()
    val name = varchar("name", 200).nullable()
    val centuryMin = integer("century_min")
    val centuryMax = integer("century_max")
    val effectiveCentury = integer("effective_century")
    val manuscriptType = varchar("manuscript_type", 20).nullable()
    val historicalNotes = text("historical_notes").nullable()
    val geographicOrigin = varchar("geographic_origin", 200).nullable()
    val discoveryLocation = varchar("discovery_location", 200).nullable()
    val ntvmrUrl = varchar("ntvmr_url", 500).nullable()
}

object ManuscriptSources : IntIdTable("manuscript_sources") {
    val manuscriptId = reference("manuscript_id", Manuscripts)
    val sourceName = varchar("source_name", 100).nullable()
    val ntvmrUrl = varchar("ntvmr_url", 500).nullable()
    val historicalNotes = text("historical_notes").nullable()
    val geographicOrigin = varchar("geographic_origin", 200).nullable()
    val discoveryLocation = varchar("discovery_location", 200).nullable()

    init {
        uniqueIndex(manuscriptId)
    }
}

object ManuscriptVerses : IntIdTable("manuscript_verses") {
    val manuscriptId = reference("manuscript_id", Manuscripts)
    val verseId = reference("verse_id", Verses)

    init {
        uniqueIndex(manuscriptId, verseId)
    }
}

object CoverageByCentury : IntIdTable("coverage_by_century") {
    val century = integer("century")
    val bookId = reference("book_id", Books)
    val coveredVerses = integer("covered_verses")
    val totalVerses = integer("total_verses")
    val coveragePercent = decimal("coverage_percent", 5, 2)

    init {
        uniqueIndex(century, bookId)
    }
}

object IngestionMetadata : Table("ingestion_metadata") {
    val id = integer("id")
    val status = varchar("status", 20)
    val startedAt = timestampWithTimeZone("started_at").nullable()
    val finishedAt = timestampWithTimeZone("finished_at").nullable()
    val durationMs = long("duration_ms").nullable()
    val manuscriptsIngested = integer("manuscripts_ingested").nullable()
    val versesLinked = integer("verses_linked").nullable()
    val errorMessage = text("error_message").nullable()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object BookTranslations : IntIdTable("book_translations") {
    val bookId = reference("book_id", Books)
    val locale = varchar("locale", 5)
    val name = varchar("name", 50)
    val abbreviation = varchar("abbreviation", 10)

    init {
        uniqueIndex(bookId, locale)
    }
}

object ChurchFathers : IntIdTable("church_fathers") {
    val displayName = varchar("display_name", 200)
    val normalizedName = varchar("normalized_name", 200).uniqueIndex()
    val centuryMin = integer("century_min")
    val centuryMax = integer("century_max")
    val shortDescription = text("short_description").nullable()
    val primaryLocation = varchar("primary_location", 200).nullable()
    val tradition = varchar("tradition", 20)
    val dataSource = varchar("source", 100).default("seed")
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    val mannerOfDeath = varchar("manner_of_death", 200).nullable()
    val biographyOriginal = text("biography_original").nullable()
    val biographySummary = text("biography_summary").nullable()
    val biographySummaryReviewed = bool("biography_summary_reviewed").default(false)
}

object FatherTextualStatements : IntIdTable("father_textual_statements") {
    val fatherId = integer("father_id").references(ChurchFathers.id)
    val topic = varchar("topic", 40)
    val statementText = text("statement_text")
    val originalLanguage = varchar("original_language", 20).nullable()
    val originalText = text("original_text").nullable()
    val sourceWork = varchar("source_work", 200).nullable()
    val sourceReference = varchar("source_reference", 200).nullable()
    val approximateYear = integer("approximate_year").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

object ChurchFatherTranslations : IntIdTable("church_father_translations") {
    val fatherId = reference("father_id", ChurchFathers)
    val locale = varchar("locale", 5)
    val displayName = varchar("display_name", 200)
    val shortDescription = text("short_description").nullable()
    val primaryLocation = varchar("primary_location", 200).nullable()
    val mannerOfDeath = varchar("manner_of_death", 200).nullable()
    val biographyOriginal = text("biography_original").nullable()
    val biographySummary = text("biography_summary").nullable()
    val translationSource = varchar("translation_source", 20).default("seed")

    init { uniqueIndex(fatherId, locale) }
}

object FatherStatementTranslations : IntIdTable("father_statement_translations") {
    val statementId = reference("statement_id", FatherTextualStatements)
    val locale = varchar("locale", 5)
    val statementText = text("statement_text")

    init { uniqueIndex(statementId, locale) }
}
