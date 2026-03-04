package com.ntcoverage.service

import com.ntcoverage.repository.ChurchFatherRepository
import com.ntcoverage.repository.ManuscriptRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
    private val churchFatherRepository: ChurchFatherRepository
) {

    private val log = LoggerFactory.getLogger(DatingEnrichmentService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val apiKey: String? = System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
    private val model: String = System.getenv("OPENAI_MODEL") ?: "gpt-4o-mini"
    private val timeoutMs: Long = System.getenv("OPENAI_TIMEOUT_MS")?.toLongOrNull() ?: 15_000L
    private val enabled: Boolean = System.getenv("ENABLE_DATING_ENRICHMENT")?.lowercase() == "true"

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = timeoutMs
        }
    }

    fun isEnabled(): Boolean = enabled && apiKey != null

    suspend fun enrichManuscriptDating(gaId: String, name: String?, centuryMin: Int, centuryMax: Int): DatingResult? {
        if (apiKey == null) {
            log.warn("DATING_ENRICHMENT: OPENAI_API_KEY not set — skipping manuscript $gaId")
            return null
        }

        val centuryRange = if (centuryMin == centuryMax) "century $centuryMin" else "centuries $centuryMin-$centuryMax"
        val manuscriptLabel = if (name != null) "$name ($gaId)" else gaId

        val systemPrompt = """You are a New Testament textual criticism and paleography scholar.
Given the Greek NT manuscript $manuscriptLabel, dated to $centuryRange:

1. Provide the scholarly dating range in years (yearMin, yearMax).
2. If there is a widely accepted specific date (e.g. "circa 125"), provide it as yearBest. If scholars only agree on a range, set yearBest to null.
3. Cite the primary scholarly reference for this dating.

Return ONLY valid JSON:
{"yearMin": N, "yearMax": N, "yearBest": N_or_null, "reference": "Author (Year). Title. ..."}"""

        val userContent = "Manuscript: $manuscriptLabel, dated to $centuryRange. Provide year-level dating."

        return callForDating(systemPrompt, userContent, "manuscript $gaId")
    }

    suspend fun enrichChurchFatherDating(displayName: String, tradition: String, centuryMin: Int, centuryMax: Int): DatingResult? {
        if (apiKey == null) {
            log.warn("DATING_ENRICHMENT: OPENAI_API_KEY not set — skipping father $displayName")
            return null
        }

        val centuryRange = if (centuryMin == centuryMax) "century $centuryMin" else "centuries $centuryMin-$centuryMax"

        val systemPrompt = """You are a patristics scholar. Given the Church Father $displayName, $tradition tradition, active in $centuryRange:

1. Provide birth year (yearMin) and death year (yearMax). Use approximate years if exact dates are unknown.
2. If there is a well-established floruit or peak activity year, provide it as yearBest. If scholars only agree on a range, set yearBest to null.
3. Cite the primary scholarly reference.

Return ONLY valid JSON:
{"yearMin": N, "yearMax": N, "yearBest": N_or_null, "reference": "Author (Year). Title. ..."}"""

        val userContent = "Church Father: $displayName, $tradition tradition, active in $centuryRange. Provide year-level dating."

        return callForDating(systemPrompt, userContent, "father $displayName")
    }

    suspend fun enrichAll(domain: String, limit: Int): List<EnrichmentReport> {
        val reports = mutableListOf<EnrichmentReport>()

        if (domain == "manuscripts" || domain == "all") {
            reports.add(enrichManuscripts(limit))
        }
        if (domain == "fathers" || domain == "all") {
            reports.add(enrichFathers(limit))
        }

        return reports
    }

    private suspend fun enrichManuscripts(limit: Int): EnrichmentReport {
        val manuscripts = manuscriptRepository.findAllWithoutDating(limit)
        var succeeded = 0
        var failed = 0
        var skipped = 0

        log.info("DATING_ENRICHMENT: starting manuscript enrichment for ${manuscripts.size} records")

        for ((index, ms) in manuscripts.withIndex()) {
            if (ms.yearMin != null) {
                skipped++
                continue
            }

            try {
                val result = enrichManuscriptDating(ms.gaId, ms.name, ms.centuryMin, ms.centuryMax)
                if (result != null) {
                    manuscriptRepository.updateDating(
                        gaId = ms.gaId,
                        yearMin = result.yearMin,
                        yearMax = result.yearMax,
                        yearBest = result.yearBest,
                        datingSource = "openai",
                        datingReference = result.reference,
                        datingConfidence = "LOW"
                    )
                    succeeded++
                    log.info("DATING_ENRICHMENT: [${index + 1}/${manuscripts.size}] manuscript ${ms.gaId}: ${result.yearMin}-${result.yearMax} (best=${result.yearBest})")
                } else {
                    failed++
                    log.warn("DATING_ENRICHMENT: [${index + 1}/${manuscripts.size}] manuscript ${ms.gaId}: no result")
                }
            } catch (e: Exception) {
                failed++
                log.error("DATING_ENRICHMENT: [${index + 1}/${manuscripts.size}] manuscript ${ms.gaId}: ${e.message}")
            }
        }

        log.info("DATING_ENRICHMENT_MANUSCRIPTS_DONE: succeeded=$succeeded, failed=$failed, skipped=$skipped")
        return EnrichmentReport(domain = "manuscripts", processed = manuscripts.size, succeeded = succeeded, failed = failed, skipped = skipped)
    }

    private suspend fun enrichFathers(limit: Int): EnrichmentReport {
        val fathers = churchFatherRepository.findAllWithoutDating(limit)
        var succeeded = 0
        var failed = 0
        var skipped = 0

        log.info("DATING_ENRICHMENT: starting church father enrichment for ${fathers.size} records")

        for ((index, father) in fathers.withIndex()) {
            if (father.yearMin != null) {
                skipped++
                continue
            }

            try {
                val result = enrichChurchFatherDating(father.displayName, father.tradition, father.centuryMin, father.centuryMax)
                if (result != null) {
                    churchFatherRepository.updateDating(
                        id = father.id,
                        yearMin = result.yearMin,
                        yearMax = result.yearMax,
                        yearBest = result.yearBest,
                        datingSource = "openai",
                        datingReference = result.reference,
                        datingConfidence = "LOW"
                    )
                    succeeded++
                    log.info("DATING_ENRICHMENT: [${index + 1}/${fathers.size}] father ${father.displayName}: ${result.yearMin}-${result.yearMax} (best=${result.yearBest})")
                } else {
                    failed++
                    log.warn("DATING_ENRICHMENT: [${index + 1}/${fathers.size}] father ${father.displayName}: no result")
                }
            } catch (e: Exception) {
                failed++
                log.error("DATING_ENRICHMENT: [${index + 1}/${fathers.size}] father ${father.displayName}: ${e.message}")
            }
        }

        log.info("DATING_ENRICHMENT_FATHERS_DONE: succeeded=$succeeded, failed=$failed, skipped=$skipped")
        return EnrichmentReport(domain = "fathers", processed = fathers.size, succeeded = succeeded, failed = failed, skipped = skipped)
    }

    private suspend fun callForDating(systemPrompt: String, userContent: String, label: String): DatingResult? {
        for (attempt in 1..2) {
            try {
                return callOpenAi(systemPrompt, userContent, label)
            } catch (e: Exception) {
                if (attempt < 2) {
                    log.warn("DATING_ENRICHMENT: attempt $attempt failed for $label, retrying: ${e.message}")
                } else {
                    log.error("DATING_ENRICHMENT: failed after 2 attempts for $label: ${e.message}")
                }
            }
        }
        return null
    }

    private suspend fun callOpenAi(systemPrompt: String, userContent: String, label: String): DatingResult {
        val requestBody = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userContent)
            ),
            temperature = 0.2
        )

        val response = client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(json.encodeToString(ChatRequest.serializer(), requestBody))
        }

        val body = response.bodyAsText()
        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("OpenAI returned ${response.status} for $label: $body")
        }

        val chatResponse = json.decodeFromString<ChatResponse>(body)
        val content = chatResponse.choices.firstOrNull()?.message?.content?.trim()
            ?: throw RuntimeException("OpenAI returned empty choices for $label")

        val jsonContent = extractJson(content)
        val parsed = json.decodeFromString<DatingResponse>(jsonContent)

        if (parsed.yearMin <= 0 || parsed.yearMax <= 0 || parsed.yearMax < parsed.yearMin) {
            throw RuntimeException("Invalid dating range for $label: ${parsed.yearMin}-${parsed.yearMax}")
        }

        return DatingResult(
            yearMin = parsed.yearMin,
            yearMax = parsed.yearMax,
            yearBest = parsed.yearBest,
            reference = parsed.reference ?: "No reference provided"
        )
    }

    private fun extractJson(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("{")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        throw RuntimeException("Could not extract JSON from response: $trimmed")
    }

    fun close() {
        client.close()
    }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.2
    )

    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: String
    )

    @Serializable
    private data class ChatResponse(
        val choices: List<ChatChoice> = emptyList()
    )

    @Serializable
    private data class ChatChoice(
        val message: ChatMessage,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    private data class DatingResponse(
        val yearMin: Int,
        val yearMax: Int,
        val yearBest: Int? = null,
        val reference: String? = null
    )
}
