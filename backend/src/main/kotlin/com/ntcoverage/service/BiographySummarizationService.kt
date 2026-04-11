package com.ntcoverage.service

import com.ntcoverage.repository.LlmQueueRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.LoggerFactory

class BiographySummarizationService(
    private val llmQueueRepository: LlmQueueRepository? = null
) {

    private val log = LoggerFactory.getLogger(BiographySummarizationService::class.java)

    private val localeToLanguage = mapOf("pt" to "Portuguese", "es" to "Spanish")

    // ── Queue-based enqueue methods (all LLM goes through the queue) ──

    private val queueJson = Json { encodeDefaults = true }

    fun enqueueBioSummarize(fatherId: Int, original: String, locale: String? = null, phaseName: String = "bio_summarize_prepare"): Int? {
        val repo = llmQueueRepository ?: return null
        if (!isLong(original)) return null

        val langInstruction = if (locale != null) {
            val lang = localeToLanguage[locale] ?: locale
            " Write the summary in $lang."
        } else ""

        val ctx = LlmResponseProcessor.BioSummarizeContext(fatherId)
        return repo.enqueue(
            phaseName = phaseName,
            label = "BIO_SUMMARIZE_$fatherId",
            systemPrompt = "Summarize this Church Father biography in neutral academic tone, 5-8 sentences. Preserve historical precision. No theological judgments.$langInstruction",
            userContent = original,
            temperature = 0.1,
            maxTokens = 800,
            tier = "MEDIUM",
            callbackContext = queueJson.encodeToString(ctx)
        )
    }

    fun enqueueBioTranslate(fatherId: Int, text: String, targetLocale: String, fatherName: String = "", phaseName: String = "bio_translate_prepare"): Int? {
        val repo = llmQueueRepository ?: return null
        val language = localeToLanguage[targetLocale] ?: return null

        val systemPrompt = "Translate this Church Father biography completely to $language. " +
            "Translate every sentence — leave nothing in English. " +
            "Preserve proper names, dates, and references. " +
            "Use academic ecclesiastical terminology. Neutral tone."

        val ctx = LlmResponseProcessor.BioTranslateContext(fatherId, targetLocale)
        return repo.enqueue(
            phaseName = phaseName,
            label = "BIO_TRANSLATE_${fatherId}_$targetLocale",
            systemPrompt = systemPrompt,
            userContent = text,
            temperature = 0.1,
            maxTokens = 1500,
            tier = "MEDIUM",
            callbackContext = queueJson.encodeToString(ctx)
        )
    }

    fun enqueueCouncilTranslate(councilId: Int, displayName: String, shortDescription: String?, location: String?, mainTopics: String?, summary: String?, targetLocale: String, phaseName: String = "council_translate_prepare"): Int? {
        val repo = llmQueueRepository ?: return null
        val language = localeToLanguage[targetLocale] ?: return null

        val fields = mutableMapOf<String, String>()
        if (displayName.isNotBlank()) fields["displayName"] = displayName
        if (!shortDescription.isNullOrBlank()) fields["shortDescription"] = shortDescription
        if (!location.isNullOrBlank()) fields["location"] = location
        if (!mainTopics.isNullOrBlank()) fields["mainTopics"] = mainTopics
        if (!summary.isNullOrBlank()) fields["summary"] = summary
        if (fields.isEmpty()) return null

        val systemPrompt = "Translate these council data fields to $language. Return ONLY valid JSON with same keys. " +
            "IMPORTANT: The 'displayName' field is the CORRECT name of this council — translate it literally, do NOT replace it with another council's name. " +
            "The 'summary' field is about THIS specific council (named in displayName) — translate only what is written, do not add information from other councils. " +
            "Use correct ecclesiastical terms: 'Council'->'Concílio'/'Concilio', 'Synod'->'Sínodo', 'Ecumenical'->'Ecumênico'/'Ecuménico'. " +
            "Use academic names for locations. Preserve dates. JSON only, no extra text."

        val userContent = buildJsonObject { fields.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }.toString()
        val ctx = LlmResponseProcessor.CouncilTranslateContext(councilId, targetLocale)
        return repo.enqueue(
            phaseName = phaseName,
            label = "COUNCIL_TRANSLATE_${councilId}_$targetLocale",
            systemPrompt = systemPrompt,
            userContent = userContent,
            temperature = 0.1,
            maxTokens = 1000,
            tier = "MEDIUM",
            callbackContext = queueJson.encodeToString(ctx)
        )
    }

    fun enqueueHeresyTranslate(heresyId: Int, name: String, description: String?, targetLocale: String, phaseName: String = "heresy_translate_prepare"): Int? {
        val repo = llmQueueRepository ?: return null
        val language = localeToLanguage[targetLocale] ?: return null

        val fields = mutableMapOf<String, String>()
        if (name.isNotBlank()) fields["name"] = name
        if (!description.isNullOrBlank()) fields["description"] = description
        if (fields.isEmpty()) return null

        val systemPrompt = "Translate these heresy data fields to $language. Return ONLY valid JSON with same keys. " +
            "Use correct ecclesiastical terminology. Preserve proper nouns. Academic tone. JSON only, no extra text."

        val userContent = buildJsonObject { fields.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }.toString()
        val ctx = LlmResponseProcessor.HeresyTranslateContext(heresyId, targetLocale)
        return repo.enqueue(
            phaseName = phaseName,
            label = "HERESY_TRANSLATE_${heresyId}_$targetLocale",
            systemPrompt = systemPrompt,
            userContent = userContent,
            temperature = 0.1,
            maxTokens = 800,
            tier = "MEDIUM",
            callbackContext = queueJson.encodeToString(ctx)
        )
    }

    fun enqueueCouncilOverview(councilId: Int, displayName: String, year: Int, location: String?, councilType: String?, mainTopics: String?, phaseName: String = "council_overview_prepare"): Int? {
        val repo = llmQueueRepository ?: return null

        val meta = buildString {
            append("Council: $displayName. Year: $year.")
            if (!location.isNullOrBlank()) append(" Location: $location.")
            if (!councilType.isNullOrBlank()) append(" Type: $councilType.")
            if (!mainTopics.isNullOrBlank()) append(" Topics: $mainTopics.")
        }

        val systemPrompt = "Write a brief 3-5 sentence overview of this ecclesiastical council based on the metadata provided. " +
            "Neutral academic tone. Do not invent facts. If the council is obscure, state that historical records are limited."

        val ctx = LlmResponseProcessor.CouncilOverviewContext(councilId)
        return repo.enqueue(
            phaseName = phaseName,
            label = "COUNCIL_OVERVIEW_$councilId",
            systemPrompt = systemPrompt,
            userContent = meta,
            temperature = 0.1,
            maxTokens = 600,
            tier = "MEDIUM",
            callbackContext = queueJson.encodeToString(ctx)
        )
    }

    companion object {
        private fun wordCount(text: String): Int = text.split("\\s+".toRegex()).size

        private fun isLong(text: String): Boolean = wordCount(text) > 120

        fun isLongBiography(text: String?): Boolean =
            text != null && isLong(text)
    }
}
