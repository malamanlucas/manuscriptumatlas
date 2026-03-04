package com.ntcoverage.model

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
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
    val yearMin = integer("year_min").nullable()
    val yearMax = integer("year_max").nullable()
    val yearBest = integer("year_best").nullable()
    val datingSource = varchar("dating_source", 100).nullable()
    val datingReference = text("dating_reference").nullable()
    val datingConfidence = varchar("dating_confidence", 10).nullable()
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
    val yearMin = integer("year_min").nullable()
    val yearMax = integer("year_max").nullable()
    val yearBest = integer("year_best").nullable()
    val datingSource = varchar("dating_source", 100).nullable()
    val datingReference = text("dating_reference").nullable()
    val datingConfidence = varchar("dating_confidence", 10).nullable()
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

object VisitorSessions : Table("visitor_sessions") {
    val id = long("id").autoIncrement()
    val visitorId = varchar("visitor_id", 36)
    val sessionId = varchar("session_id", 36)
    val ipAddress = varchar("ip_address", 100)
    val userAgent = text("user_agent")
    val browserName = varchar("browser_name", 50).nullable()
    val browserVersion = varchar("browser_version", 30).nullable()
    val osName = varchar("os_name", 50).nullable()
    val osVersion = varchar("os_version", 30).nullable()
    val deviceType = varchar("device_type", 10).nullable()
    val screenWidth = short("screen_width").nullable()
    val screenHeight = short("screen_height").nullable()
    val viewportWidth = short("viewport_width").nullable()
    val viewportHeight = short("viewport_height").nullable()
    val language = varchar("language", 10).nullable()
    val languages = text("languages").nullable()
    val timezone = varchar("timezone", 50).nullable()
    val platform = varchar("platform", 50).nullable()
    val networkInfo = text("network_info").nullable()
    val deviceMemory = short("device_memory").nullable()
    val hardwareConcurrency = short("hardware_concurrency").nullable()
    val colorDepth = short("color_depth").nullable()
    val pixelRatio = decimal("pixel_ratio", 4, 2).nullable()
    val touchPoints = short("touch_points").nullable()
    val cookieEnabled = bool("cookie_enabled").nullable()
    val doNotTrack = bool("do_not_track").nullable()
    val webglRenderer = varchar("webgl_renderer", 200).nullable()
    val webglVendor = varchar("webgl_vendor", 200).nullable()
    val canvasFingerprint = varchar("canvas_fingerprint", 64).nullable()
    val referrer = text("referrer").nullable()
    val pageLoadTimeMs = integer("page_load_time_ms").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val lastActivityAt = timestampWithTimeZone("last_activity_at")

    override val primaryKey = PrimaryKey(id, createdAt)
}

object PageViews : Table("page_views") {
    val id = long("id").autoIncrement()
    val sessionId = varchar("session_id", 36)
    val visitorId = varchar("visitor_id", 36)
    val path = varchar("path", 500)
    val referrerPath = varchar("referrer_path", 500).nullable()
    val durationMs = integer("duration_ms").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id, createdAt)
}

object Users : IntIdTable("users") {
    val email = varchar("email", 200).uniqueIndex()
    val displayName = varchar("display_name", 200)
    val pictureUrl = varchar("picture_url", 500).nullable()
    val role = enumerationByName("role", 20, UserRole::class)
    val createdAt = timestampWithTimeZone("created_at")
    val lastLoginAt = timestampWithTimeZone("last_login_at").nullable()
}

object VisitorDailyStats : IntIdTable("visitor_daily_stats") {
    val statDate = date("stat_date").uniqueIndex()
    val totalSessions = integer("total_sessions").default(0)
    val totalPageviews = integer("total_pageviews").default(0)
    val uniqueVisitors = integer("unique_visitors").default(0)
    val avgSessionDurationMs = integer("avg_session_duration_ms").nullable()
    val topBrowsers = text("top_browsers").nullable()
    val topOs = text("top_os").nullable()
    val topDevices = text("top_devices").nullable()
    val topPages = text("top_pages").nullable()
    val topCountries = text("top_countries").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}
