package com.ntcoverage.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object InterlinearGlossAudits : IntIdTable("interlinear_gloss_audits") {
    val wordId = reference("word_id", InterlinearWords)
    val glossSnapshot = text("gloss_snapshot")
    val verdict = varchar("verdict", 16)
    val suggestedPt = text("suggested_pt").nullable()
    val reason = text("reason").nullable()
    val judgeModel = varchar("judge_model", 64).nullable()
    val judgedAt = timestampWithTimeZone("judged_at")
    val resolvedAt = timestampWithTimeZone("resolved_at").nullable()
}

object GlossAuditVerdict {
    const val OK = "ok"
    const val BAD_EN = "bad_en"
    const val BAD_ES = "bad_es"
    const val BAD_OTHER = "bad_other"
    const val UNKNOWN = "unknown"

    val BAD = setOf(BAD_EN, BAD_ES, BAD_OTHER)
    val ALL = setOf(OK, BAD_EN, BAD_ES, BAD_OTHER, UNKNOWN)
}

@Serializable
data class GlossAuditStatsDTO(
    val total: Int,
    val ok: Int,
    val badEn: Int,
    val badEs: Int,
    val badOther: Int,
    val unknown: Int,
    val pending: Int,
    val examples: List<GlossAuditExampleDTO>
)

@Serializable
data class GlossAuditExampleDTO(
    val wordId: Int,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val wordPosition: Int,
    val originalWord: String,
    val transliteration: String?,
    val englishGloss: String?,
    val currentGloss: String,
    val verdict: String,
    val suggestedPt: String?,
    val reason: String?
)
