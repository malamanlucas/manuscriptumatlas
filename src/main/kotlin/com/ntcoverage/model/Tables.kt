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
