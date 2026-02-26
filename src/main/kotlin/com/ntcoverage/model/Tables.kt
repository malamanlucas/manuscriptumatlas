package com.ntcoverage.model

import org.jetbrains.exposed.dao.id.IntIdTable

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
