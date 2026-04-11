package com.ntcoverage.service

import com.ntcoverage.repository.ChurchFatherRepository
import com.ntcoverage.repository.LlmQueueRepository
import com.ntcoverage.repository.ManuscriptRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

data class DatingResult(
    val yearMin: Int,
    val yearMax: Int,
    val yearBest: Int?,
    val reference: String
)

@Serializable
data class EnrichmentReport(
    val domain: String,
    val processed: Int,
    val succeeded: Int,
    val failed: Int,
    val skipped: Int
)

class DatingEnrichmentService(
    private val manuscriptRepository: ManuscriptRepository,
    private val churchFatherRepository: ChurchFatherRepository,
    private val llmQueueRepository: LlmQueueRepository? = null
) {

    private val log = LoggerFactory.getLogger(DatingEnrichmentService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val enabled: Boolean = System.getenv("ENABLE_DATING_ENRICHMENT")?.lowercase() == "true"

    fun isEnabled(): Boolean = enabled && llmQueueRepository != null

    // ── Queue-based enqueue methods (all LLM goes through the queue) ──

    private val queueJson = Json { encodeDefaults = true }

    fun enqueueManuscriptDating(limit: Int): Int {
        val repo = llmQueueRepository ?: throw IllegalStateException("llmQueueRepository not configured")
        val manuscripts = manuscriptRepository.findAllWithoutDating(limit)
        var enqueued = 0

        for (ms in manuscripts) {
            if (ms.yearMin != null) continue
            val centuryRange = if (ms.centuryMin == ms.centuryMax) "century ${ms.centuryMin}" else "centuries ${ms.centuryMin}-${ms.centuryMax}"
            val manuscriptLabel = if (ms.name != null) "${ms.name} (${ms.gaId})" else ms.gaId

            val systemPrompt = """You are a New Testament textual criticism and paleography scholar.
Given the Greek NT manuscript $manuscriptLabel, dated to $centuryRange:

1. Provide the scholarly dating range in years (yearMin, yearMax).
2. If there is a widely accepted specific date (e.g. "circa 125"), provide it as yearBest. If scholars only agree on a range, set yearBest to null.
3. Cite the primary scholarly reference for this dating.

Return ONLY valid JSON:
{"yearMin": N, "yearMax": N, "yearBest": N_or_null, "reference": "Author (Year). Title. ..."}"""

            val userContent = "Manuscript: $manuscriptLabel, dated to $centuryRange. Provide year-level dating."
            val ctx = LlmResponseProcessor.ManuscriptDatingContext(ms.gaId)

            repo.enqueue(
                phaseName = "dating_manuscripts_prepare",
                label = "DatingEnrichment:manuscript ${ms.gaId}",
                systemPrompt = systemPrompt,
                userContent = userContent,
                temperature = 0.0,
                maxTokens = 500,
                tier = "HIGH",
                callbackContext = queueJson.encodeToString(LlmResponseProcessor.ManuscriptDatingContext.serializer(), ctx)
            )
            enqueued++
        }
        log.info("DATING_ENQUEUE: enqueued $enqueued manuscripts for dating")
        return enqueued
    }

    fun enqueueFatherDating(limit: Int): Int {
        val repo = llmQueueRepository ?: throw IllegalStateException("llmQueueRepository not configured")
        val fathers = churchFatherRepository.findAllWithoutDating(limit)
        var enqueued = 0

        for (father in fathers) {
            if (father.yearMin != null) continue
            val centuryRange = if (father.centuryMin == father.centuryMax) "century ${father.centuryMin}" else "centuries ${father.centuryMin}-${father.centuryMax}"

            val systemPrompt = """You are a patristics scholar. Given the Church Father ${father.displayName}, ${father.tradition} tradition, active in $centuryRange:

1. Provide birth year (yearMin) and death year (yearMax). Use approximate years if exact dates are unknown.
2. If there is a well-established floruit or peak activity year, provide it as yearBest. If scholars only agree on a range, set yearBest to null.
3. Cite the primary scholarly reference.

Return ONLY valid JSON:
{"yearMin": N, "yearMax": N, "yearBest": N_or_null, "reference": "Author (Year). Title. ..."}"""

            val userContent = "Church Father: ${father.displayName}, ${father.tradition} tradition, active in $centuryRange. Provide year-level dating."
            val ctx = LlmResponseProcessor.FatherDatingContext(father.id)

            repo.enqueue(
                phaseName = "dating_fathers_prepare",
                label = "DatingEnrichment:father ${father.displayName}",
                systemPrompt = systemPrompt,
                userContent = userContent,
                temperature = 0.0,
                maxTokens = 500,
                tier = "HIGH",
                callbackContext = queueJson.encodeToString(LlmResponseProcessor.FatherDatingContext.serializer(), ctx)
            )
            enqueued++
        }
        log.info("DATING_ENQUEUE: enqueued $enqueued fathers for dating")
        return enqueued
    }
}
