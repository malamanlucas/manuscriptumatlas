package com.ntcoverage.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object BibleLayer4Applications : IntIdTable("bible_layer4_applications") {
    val phaseName = varchar("phase_name", 64)
    val bookName = varchar("book_name", 40).nullable()
    val chapter = integer("chapter").nullable()
    val verse = integer("verse").nullable()
    val status = varchar("status", 16).default("running")
    val itemsProcessed = integer("items_processed").default(0)
    val enqueuedCount = integer("enqueued_count").default(0)
    val errorMessage = text("error_message").nullable()
    val requestedAt = timestamp("requested_at").clientDefault { Instant.now() }
    val finishedAt = timestamp("finished_at").nullable()
}

@Serializable
data class BibleLayer4ApplicationDTO(
    val id: Int,
    val phaseName: String,
    val bookName: String?,
    val chapter: Int?,
    val verse: Int?,
    val status: String,
    val itemsProcessed: Int,
    val enqueuedCount: Int,
    val errorMessage: String?,
    val requestedAt: String,
    val finishedAt: String?
)

@Serializable
data class RunScopedRequest(
    val phases: List<String>,
    val bookName: String? = null,
    val chapter: Int? = null,
    val verse: Int? = null,
    val locales: List<String>? = null
)

@Serializable
data class RunPhaseScopedRequest(
    val bookName: String? = null,
    val chapter: Int? = null,
    val verse: Int? = null,
    val locales: List<String>? = null
)

@Serializable
data class RunScopedResponse(
    val message: String,
    val applicationIds: List<Int>
)

@Serializable
data class BibleLayer4VerseCoverageDTO(
    val verse: Int,
    val tokenizeArc69: Boolean,
    val tokenizeKjv: Boolean,
    val alignKjv: Boolean,
    val alignArc69: Boolean,
    val enrichSemanticsArc69: Boolean
)

@Serializable
data class BibleLayer4CoverageDTO(
    val book: String,
    val chapter: Int,
    val totalVerses: Int,
    val verses: List<BibleLayer4VerseCoverageDTO>
)
