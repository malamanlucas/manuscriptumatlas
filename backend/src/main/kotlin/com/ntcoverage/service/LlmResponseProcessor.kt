package com.ntcoverage.service

import com.ntcoverage.repository.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

private fun cleanLlmJsonResponse(raw: String): String {
    var s = raw.trim()
    // Remove markdown code fences
    s = s.removeSurrounding("```json", "```").removeSurrounding("```", "```").trim()
    // Unescape double-escaped content from LLM responses: \n → newline, \" → "
    if (s.startsWith("\\n") || s.contains("\\\"")) {
        s = s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\/", "/").trim()
    }
    // Extract JSON object from surrounding text
    val start = s.indexOf('{')
    val end = s.lastIndexOf('}')
    if (start >= 0 && end > start) {
        s = s.substring(start, end + 1)
    }
    return s
}

class LlmResponseProcessor(
    private val queueRepository: LlmQueueRepository,
    private val councilRepository: CouncilRepository,
    private val lexiconRepository: LexiconRepository,
    private val interlinearRepository: InterlinearRepository,
    private val manuscriptRepository: ManuscriptRepository,
    private val churchFatherRepository: ChurchFatherRepository,
    private val heresyRepository: HeresyRepository,
    private val apologeticTopicRepository: ApologeticTopicRepository? = null,
    private val apologeticResponseRepository: ApologeticResponseRepository? = null
) {
    private val log = LoggerFactory.getLogger(LlmResponseProcessor::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    data class ProcessResult(val applied: Int, val errors: Int)

    fun processCompleted(phaseName: String): ProcessResult {
        val items = queueRepository.getCompletedByPhase(phaseName)
        if (items.isEmpty()) {
            log.info("LLM_PROCESSOR: no completed items for phase={}", phaseName)
            return ProcessResult(0, 0)
        }

        log.info("LLM_PROCESSOR: processing {} completed items for phase={}", items.size, phaseName)
        var applied = 0
        var errors = 0

        for (item in items) {
            try {
                when {
                    item.label.startsWith("ConflictResolution:") -> applyConflictResolution(item)
                    item.label.startsWith("LEXICON_TRANSLATE_") -> applyLexiconTranslation(item)
                    item.label.startsWith("LEXICON_BATCH_") -> applyLexiconBatch(item)
                    item.label.startsWith("GLOSS_TRANSLATE_") -> applyGlossTranslation(item)
                    item.label.startsWith("ENRICHMENT_TRANSLATE_") -> applyEnrichmentTranslation(item)
                    item.label.startsWith("WORD_ALIGN_") -> applyWordAlignment(item)
                    item.label.startsWith("SEMANTIC_ENRICH_") -> applySemanticEnrichment(item)
                    item.label.startsWith("DatingEnrichment:manuscript") -> applyManuscriptDating(item)
                    item.label.startsWith("DatingEnrichment:father") -> applyFatherDating(item)
                    item.label.startsWith("BIO_SUMMARIZE_") -> applyBioSummarization(item)
                    item.label.startsWith("BIO_TRANSLATE_") -> applyBioTranslation(item)
                    item.label.startsWith("COUNCIL_TRANSLATE_") -> applyCouncilTranslation(item)
                    item.label.startsWith("HERESY_TRANSLATE_") -> applyHeresyTranslation(item)
                    item.label.startsWith("COUNCIL_OVERVIEW_") -> applyCouncilOverview(item)
                    item.label.startsWith("APOLOGETICS_TOPIC_") -> applyApologeticsTopic(item)
                    item.label.startsWith("APOLOGETICS_RESPONSE_") -> applyApologeticsResponse(item)
                    else -> {
                        log.warn("LLM_PROCESSOR: unknown label={} in phase={}", item.label, phaseName)
                        continue
                    }
                }
                queueRepository.markApplied(item.id)
                applied++
            } catch (e: Exception) {
                log.error("LLM_PROCESSOR: failed to apply id={} label={} error={}", item.id, item.label, e.message)
                queueRepository.markFailed(item.id, "Apply error: ${e.message}")
                errors++
            }
        }

        log.info("LLM_PROCESSOR: phase={} applied={} errors={}", phaseName, applied, errors)
        return ProcessResult(applied, errors)
    }

    // ── Conflict Resolution ──

    private fun applyConflictResolution(item: com.ntcoverage.model.QueueItemDTO) {
        val context = item.callbackContext
            ?: throw IllegalStateException("Missing callbackContext for ConflictResolution item id=${item.id}")
        val ctx = json.decodeFromString(ConflictContext.serializer(), context)
        val response = item.responseContent
            ?: throw IllegalStateException("Missing responseContent for item id=${item.id}")

        val payload = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val resolution = try {
            json.decodeFromString(ResolutionPayload.serializer(), payload)
        } catch (_: Exception) {
            val chosen = Regex("chosen[_\\s]?value\"?\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1) ?: ""
            val reason = Regex("reason\"?\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1) ?: payload.take(300)
            val confidence = Regex("confidence\"?\\s*:\\s*([0-9.]+)").find(payload)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.5
            ResolutionPayload(chosen, reason, confidence)
        }

        when (ctx.field) {
            "year" -> {
                val year = resolution.chosenValue.toIntOrNull()
                if (year != null) {
                    councilRepository.updateConflictResolution(ctx.councilId, ctx.field, year.toString(), resolution.reason, resolution.confidence.coerceIn(0.0, 1.0))
                }
            }
            "yearEnd", "location", "participants" -> {
                councilRepository.updateConflictResolution(ctx.councilId, ctx.field, resolution.chosenValue, resolution.reason, resolution.confidence.coerceIn(0.0, 1.0))
            }
        }
        log.info("LLM_PROCESSOR: applied conflict resolution councilId={} field={} value={}", ctx.councilId, ctx.field, resolution.chosenValue)
    }

    // ── Lexicon Translation (single) ──

    private fun applyLexiconTranslation(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(LexiconTranslateContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return

        val shortMatch = Regex("SHORT:\\s*(.+?)(?=\\nFULL:|$)", RegexOption.DOT_MATCHES_ALL).find(content)
        val fullMatch = Regex("FULL:\\s*(.+)", RegexOption.DOT_MATCHES_ALL).find(content)
        val shortDef = shortMatch?.groupValues?.get(1)?.trim()
        val fullDef = fullMatch?.groupValues?.get(1)?.trim()

        if (shortDef == null && fullDef == null) return

        if (ctx.lexiconType == "greek") {
            lexiconRepository.upsertGreekTranslation(ctx.entryId, ctx.locale, shortDef, fullDef)
        } else {
            lexiconRepository.upsertHebrewTranslation(ctx.entryId, ctx.locale, shortDef, fullDef)
        }
        log.info("LLM_PROCESSOR: applied lexicon translation entry={} locale={}", ctx.entryId, ctx.locale)
    }

    // ── Lexicon Batch ──

    private fun applyLexiconBatch(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(LexiconBatchContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return

        val blockPattern = Regex("""\[([GH]\d+)\]\s*\n\s*SHORT:\s*(.+?)(?=\nFULL:)?\nFULL:\s*(.+?)(?=\n\[|$)""", RegexOption.DOT_MATCHES_ALL)
        val matches = blockPattern.findAll(content)

        val strongsToId = ctx.entries.associate { it.strongsNumber to it.id }
        val translations = mutableListOf<Triple<Int, String?, String?>>()

        for (match in matches) {
            val strongsNumber = match.groupValues[1]
            val shortDef = match.groupValues[2].trim().takeIf { it.isNotBlank() }
            val fullDef = match.groupValues[3].trim().takeIf { it.isNotBlank() }
            val id = strongsToId[strongsNumber] ?: continue
            translations.add(Triple(id, shortDef, fullDef))
        }

        if (ctx.lexiconType == "greek") {
            for ((id, shortDef, fullDef) in translations) {
                lexiconRepository.upsertGreekTranslation(id, ctx.locale, shortDef, fullDef)
            }
        } else {
            lexiconRepository.batchUpsertHebrewTranslations(translations, ctx.locale)
        }
        log.info("LLM_PROCESSOR: applied lexicon batch type={} locale={} count={}", ctx.lexiconType, ctx.locale, translations.size)
    }

    // ── Gloss Translation ──

    private fun applyGlossTranslation(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(GlossTranslateContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return

        val glossMap = parseGlossJson(content, ctx.keys)
        if (glossMap.isEmpty()) return

        var applied = 0
        for ((key, translation) in glossMap) {
            val entryCtx = ctx.entries.find { it.transliteration == key } ?: continue
            if (ctx.locale == "pt") {
                interlinearRepository.updateGlosses(entryCtx.wordId, translation, null)
            } else if (ctx.locale == "es") {
                interlinearRepository.updateGlosses(entryCtx.wordId, null, translation)
            }
            applied++
        }
        log.info("LLM_PROCESSOR: applied gloss translations locale={} count={}", ctx.locale, applied)
    }

    private fun parseGlossJson(content: String, keys: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val cleaned = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start < 0 || end <= start) return result
            val jsonStr = cleaned.substring(start, end + 1)
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            for (key in keys) {
                val value = obj[key]?.jsonPrimitive?.content
                if (value != null && value.isNotBlank()) {
                    result[key] = value
                }
            }
        } catch (e: Exception) {
            log.warn("LLM_PROCESSOR: failed to parse gloss JSON: {}", e.message)
        }
        return result
    }

    // ── Enrichment Translation ──

    private fun applyEnrichmentTranslation(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(EnrichmentTranslateContext.serializer(), item.callbackContext!!)
        val content = item.responseContent ?: return

        val kjvTrans = extractFieldValue(content, "KJV_TRANSLATION")
        val originTrans = extractFieldValue(content, "WORD_ORIGIN")
        val exhaustiveTrans = extractFieldValue(content, "STRONGS_EXHAUSTIVE")
        val nasOriginTrans = extractFieldValue(content, "NAS_ORIGIN")
        val nasDefTrans = extractFieldValue(content, "NAS_DEFINITION")
        val nasTransTrans = extractFieldValue(content, "NAS_TRANSLATION")

        val allNull = listOf(kjvTrans, originTrans, exhaustiveTrans, nasOriginTrans, nasDefTrans, nasTransTrans).all { it == null }
        if (allNull) {
            log.warn("LLM_PROCESSOR: enrichment translation has no extractable fields, entry={} locale={}, skipping. responsePreview='{}'", ctx.entryId, ctx.locale, content.take(200))
            return
        }

        if (ctx.lexiconType == "greek") {
            lexiconRepository.upsertGreekEnrichmentTranslation(ctx.entryId, ctx.locale, kjvTrans, originTrans, exhaustiveTrans, nasOriginTrans, nasDefTrans, nasTransTrans)
        } else {
            lexiconRepository.upsertHebrewEnrichmentTranslation(ctx.entryId, ctx.locale, kjvTrans, originTrans, exhaustiveTrans, nasOriginTrans, nasDefTrans, nasTransTrans)
        }
        log.info("LLM_PROCESSOR: applied enrichment translation entry={} locale={}", ctx.entryId, ctx.locale)
    }

    private fun extractFieldValue(content: String, fieldName: String): String? {
        val pattern = Regex("$fieldName:\\s*(.+?)(?=\\n[A-Z_]+:|$)", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(content)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    // ── Word Alignment ──

    private fun applyWordAlignment(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(WordAlignContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return

        val jsonContent = extractJson(content) ?: return
        val parsed = try {
            json.decodeFromString(AlignmentResponse.serializer(), jsonContent)
        } catch (e: Exception) {
            log.warn("LLM_PROCESSOR: failed to parse alignment JSON for verse={}: {}", ctx.verseId, e.message)
            return
        }

        for (wa in parsed.a) {
            val kjvIndicesJson = wa.k?.let { "[${it.joinToString(",")}]" }
            val confidence = (wa.c ?: 50).coerceIn(0, 100)
            val divergent = confidence < 60
            interlinearRepository.upsertAlignment(
                verseId = ctx.verseId,
                wordPosition = wa.g.toShort(),
                versionCode = ctx.versionCode,
                kjvIndices = kjvIndicesJson,
                alignedText = wa.t,
                isDivergent = divergent,
                confidence = confidence
            )
        }
        log.info("LLM_PROCESSOR: applied word alignment verseId={} version={} count={}", ctx.verseId, ctx.versionCode, parsed.a.size)
    }

    // ── Dating (Manuscripts) ──

    private fun applyManuscriptDating(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(ManuscriptDatingContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return
        val jsonContent = extractJson(content) ?: return

        val parsed = json.decodeFromString(DatingResponse.serializer(), jsonContent)
        if (parsed.yearMin <= 0 || parsed.yearMax <= 0 || parsed.yearMax < parsed.yearMin) {
            log.warn("LLM_PROCESSOR: invalid dating range for manuscript {}: {}-{}", ctx.gaId, parsed.yearMin, parsed.yearMax)
            return
        }

        manuscriptRepository.updateDating(
            gaId = ctx.gaId,
            yearMin = parsed.yearMin,
            yearMax = parsed.yearMax,
            yearBest = parsed.yearBest,
            datingSource = "llm",
            datingReference = parsed.reference ?: "No reference provided",
            datingConfidence = "LOW"
        )
        log.info("LLM_PROCESSOR: applied manuscript dating gaId={} range={}-{}", ctx.gaId, parsed.yearMin, parsed.yearMax)
    }

    // ── Dating (Church Fathers) ──

    private fun applyFatherDating(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(FatherDatingContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return
        val jsonContent = extractJson(content) ?: return

        val parsed = json.decodeFromString(DatingResponse.serializer(), jsonContent)
        if (parsed.yearMin <= 0 || parsed.yearMax <= 0 || parsed.yearMax < parsed.yearMin) {
            log.warn("LLM_PROCESSOR: invalid dating range for father {}: {}-{}", ctx.fatherId, parsed.yearMin, parsed.yearMax)
            return
        }

        churchFatherRepository.updateDating(
            id = ctx.fatherId,
            yearMin = parsed.yearMin,
            yearMax = parsed.yearMax,
            yearBest = parsed.yearBest,
            datingSource = "llm",
            datingReference = parsed.reference ?: "No reference provided",
            datingConfidence = "LOW"
        )
        log.info("LLM_PROCESSOR: applied father dating id={} range={}-{}", ctx.fatherId, parsed.yearMin, parsed.yearMax)
    }

    // ── Biography Summarization ──

    private fun applyBioSummarization(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(BioSummarizeContext.serializer(), item.callbackContext!!)
        val summary = item.responseContent?.trim() ?: return

        churchFatherRepository.updateBiographySummary(ctx.fatherId, summary)
        log.info("LLM_PROCESSOR: applied bio summarization fatherId={}", ctx.fatherId)
    }

    // ── Biography Translation ──

    private fun applyBioTranslation(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(BioTranslateContext.serializer(), item.callbackContext!!)
        val translated = item.responseContent?.trim() ?: return

        churchFatherRepository.insertTranslation(
            fatherId = ctx.fatherId,
            locale = ctx.locale,
            displayName = ctx.displayName,
            shortDescription = ctx.shortDescription,
            primaryLocation = ctx.primaryLocation,
            biographyOriginal = translated,
            biographySummary = null,
            translationSource = "machine"
        )
        log.info("LLM_PROCESSOR: applied bio translation fatherId={} locale={}", ctx.fatherId, ctx.locale)
    }

    // ── Council Translation ──

    private fun applyCouncilTranslation(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(CouncilTranslateContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return

        val clean = cleanLlmJsonResponse(content)
        val obj = try {
            json.parseToJsonElement(clean).jsonObject
        } catch (e: Exception) {
            log.warn("LLM_PROCESSOR: failed to parse council translation JSON: {}", e.message)
            return
        }
        val parsed = obj.mapValues { (_, v) -> v.jsonPrimitive.content }

        val displayName = parsed["displayName"] ?: return
        councilRepository.insertOrUpdateTranslation(
            councilId = ctx.councilId,
            locale = ctx.locale,
            displayName = displayName,
            shortDescription = parsed["shortDescription"],
            location = parsed["location"],
            mainTopics = parsed["mainTopics"],
            summary = parsed["summary"],
            translationSource = "machine"
        )
        log.info("LLM_PROCESSOR: applied council translation councilId={} locale={}", ctx.councilId, ctx.locale)
    }

    // ── Heresy Translation ──

    private fun applyHeresyTranslation(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(HeresyTranslateContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return

        val clean = cleanLlmJsonResponse(content)
        val obj = try {
            json.parseToJsonElement(clean).jsonObject
        } catch (e: Exception) {
            log.warn("LLM_PROCESSOR: failed to parse heresy translation JSON: {}", e.message)
            return
        }
        val parsed = obj.mapValues { (_, v) -> v.jsonPrimitive.content }

        val name = parsed["name"] ?: return
        heresyRepository.insertOrUpdateTranslation(
            heresyId = ctx.heresyId,
            locale = ctx.locale,
            name = name,
            description = parsed["description"],
            translationSource = "machine"
        )
        log.info("LLM_PROCESSOR: applied heresy translation heresyId={} locale={}", ctx.heresyId, ctx.locale)
    }

    // ── Council Overview ──

    private fun applyCouncilOverview(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(CouncilOverviewContext.serializer(), item.callbackContext!!)
        val overview = item.responseContent?.trim() ?: return

        councilRepository.updateSummary(ctx.councilId, overview, reviewed = false)
        log.info("LLM_PROCESSOR: applied council overview councilId={}", ctx.councilId)
    }

    // ── Utilities ──

    private fun extractJson(content: String): String? {
        val trimmed = content.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        log.warn("LLM_PROCESSOR: could not extract JSON from response")
        return null
    }

    // ── Context DTOs ──

    @Serializable
    private data class ConflictContext(val councilId: Int, val councilName: String, val field: String)

    @Serializable
    private data class ResolutionPayload(
        @SerialName("chosen_value") val chosenValue: String,
        val reason: String,
        val confidence: Double
    )

    @Serializable
    data class LexiconTranslateContext(val entryId: Int, val locale: String, val lexiconType: String)

    @Serializable
    data class LexiconBatchContext(
        val locale: String,
        val lexiconType: String,
        val entries: List<LexiconBatchEntry>
    )

    @Serializable
    data class LexiconBatchEntry(val id: Int, val strongsNumber: String)

    @Serializable
    data class GlossTranslateContext(
        val locale: String,
        val keys: List<String>,
        val entries: List<GlossEntryContext>
    )

    @Serializable
    data class GlossEntryContext(val transliteration: String, val wordId: Int)

    @Serializable
    data class EnrichmentTranslateContext(val entryId: Int, val locale: String, val lexiconType: String)

    @Serializable
    data class WordAlignContext(val verseId: Int, val versionCode: String, val bookName: String, val chapter: Int, val verseNumber: Int)

    @Serializable
    data class SemanticEnrichContext(
        val verseId: Int,
        val versionCode: String,
        val bookName: String,
        val chapter: Int,
        val verseNumber: Int
    )

    @Serializable
    data class ManuscriptDatingContext(val gaId: String)

    @Serializable
    data class FatherDatingContext(val fatherId: Int)

    @Serializable
    data class BioSummarizeContext(val fatherId: Int)

    @Serializable
    data class BioTranslateContext(
        val fatherId: Int,
        val locale: String,
        val displayName: String = "",
        val shortDescription: String? = null,
        val primaryLocation: String? = null
    )

    @Serializable
    data class CouncilTranslateContext(val councilId: Int, val locale: String)

    @Serializable
    data class HeresyTranslateContext(val heresyId: Int, val locale: String)

    @Serializable
    data class CouncilOverviewContext(val councilId: Int)

    @Serializable
    private data class DatingResponse(
        val yearMin: Int,
        val yearMax: Int,
        val yearBest: Int? = null,
        val reference: String? = null
    )

    @Serializable
    private data class AlignmentResponse(val a: List<AlignmentEntry>)

    @Serializable
    private data class AlignmentEntry(
        val g: Int,
        val k: List<Int>? = null,
        val t: String? = null,
        val c: Int? = null
    )

    @Serializable
    private data class SemanticEnrichResponse(val e: List<SemanticEnrichEntry>)

    @Serializable
    private data class SemanticEnrichEntry(
        val g: Int,                        // Greek word position
        val s: String? = null,             // contextual sense
        val r: String? = null              // semantic_relation: equivalent|synonymous|related|divergent
    )

    // ── Semantic Enrichment (N4b) ──

    private fun applySemanticEnrichment(item: com.ntcoverage.model.QueueItemDTO) {
        val ctx = json.decodeFromString(SemanticEnrichContext.serializer(), item.callbackContext!!)
        val content = item.responseContent?.trim() ?: return

        val jsonContent = cleanLlmJsonResponse(content)
        val parsed = try {
            json.decodeFromString<SemanticEnrichResponse>(jsonContent)
        } catch (e: Exception) {
            log.warn("LLM_PROCESSOR: failed to parse semantic enrichment JSON for verse={}: {}", ctx.verseId, e.message)
            return
        }

        val validRelations = setOf("equivalent", "synonymous", "related", "divergent")
        var updated = 0
        for (entry in parsed.e) {
            val relation = entry.r?.takeIf { it in validRelations }
            if (entry.s == null && relation == null) continue
            interlinearRepository.updateSemanticFields(
                verseId = ctx.verseId,
                wordPosition = entry.g.toShort(),
                versionCode = ctx.versionCode,
                contextualSense = entry.s,
                semanticRelation = relation
            )
            updated++
        }
        log.info("LLM_PROCESSOR: applied semantic enrichment verseId={} version={} count={}", ctx.verseId, ctx.versionCode, updated)
    }

    // ── Apologetics handlers ────────────────────────────────

    private fun applyApologeticsTopic(item: com.ntcoverage.model.QueueItemDTO) {
        val repo = apologeticTopicRepository
            ?: throw IllegalStateException("ApologeticTopicRepository not configured in LlmResponseProcessor")
        val ctx = json.decodeFromString<ApologeticsTopicContext>(item.callbackContext ?: "{}")
        val (title, body) = ApologeticsService.parseTopicResult(item.responseContent ?: "")
        val updated = repo.update(ctx.topicId, title = title, body = body, status = "DRAFT", bodyReviewed = null)
        if (updated) {
            log.info("LLM_PROCESSOR: apologetics topic applied, topicId={}, title='{}'", ctx.topicId, title.take(60))
        } else {
            log.warn("LLM_PROCESSOR: apologetics topic not found, topicId={}", ctx.topicId)
        }
    }

    private fun applyApologeticsResponse(item: com.ntcoverage.model.QueueItemDTO) {
        val repo = apologeticResponseRepository
            ?: throw IllegalStateException("ApologeticResponseRepository not configured in LlmResponseProcessor")
        val ctx = json.decodeFromString<ApologeticsResponseContext>(item.callbackContext ?: "{}")
        val enrichedBody = item.responseContent ?: return
        val updated = repo.update(ctx.responseId, body = enrichedBody, bodyReviewed = null)
        if (updated) {
            log.info("LLM_PROCESSOR: apologetics response applied, responseId={}", ctx.responseId)
        } else {
            log.warn("LLM_PROCESSOR: apologetics response not found, responseId={}", ctx.responseId)
        }
    }

    @Serializable
    private data class ApologeticsTopicContext(val topicId: Int)

    @Serializable
    private data class ApologeticsResponseContext(val responseId: Int, val topicId: Int? = null)
}
