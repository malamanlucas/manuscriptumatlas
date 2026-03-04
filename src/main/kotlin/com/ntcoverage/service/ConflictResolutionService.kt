package com.ntcoverage.service

import com.ntcoverage.model.SourceDTO
import com.ntcoverage.repository.SourceClaimWithMeta
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ConflictResolutionService {
    data class Resolution(
        val chosenValue: String,
        val justification: String,
        val confidence: Double
    )

    private val log = LoggerFactory.getLogger(ConflictResolutionService::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val apiKey = System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
    private val model = System.getenv("OPENAI_MODEL") ?: "gpt-4o-mini"
    private val enabled = System.getenv("ENABLE_COUNCIL_CONFLICT_AI")?.lowercase() != "false"

    private val client = HttpClient(CIO) {
        engine { requestTimeout = 20_000 }
    }

    suspend fun resolveConflict(
        councilId: Int,
        claims: List<SourceClaimWithMeta>,
        sources: List<SourceDTO>,
        field: String,
        councilName: String = "Unknown council"
    ): Resolution {
        if (claims.isEmpty()) {
            return Resolution(chosenValue = "", justification = "No claims available", confidence = 0.0)
        }

        if (!enabled || apiKey == null) {
            val fallback = claims.maxByOrNull { it.sourceBaseWeight }
            return Resolution(
                chosenValue = extractFieldValue(field, fallback) ?: "",
                justification = "Fallback to highest weighted source without AI.",
                confidence = 0.55
            )
        }

        return try {
            val sourceLookup = sources.associateBy { it.id }
            val prompt = buildPrompt(councilId, councilName, claims, sourceLookup, field)
            val raw = callOpenAi(prompt)
            parseResolution(raw)
        } catch (e: Exception) {
            log.warn("CONFLICT_RESOLUTION_AI_FAILED councilId={} field={} error={}", councilId, field, e.message)
            val fallback = claims.maxByOrNull { it.sourceBaseWeight }
            Resolution(
                chosenValue = extractFieldValue(field, fallback) ?: "",
                justification = "AI resolution failed; used highest weighted source.",
                confidence = 0.5
            )
        }
    }

    private fun buildPrompt(
        councilId: Int,
        councilName: String,
        claims: List<SourceClaimWithMeta>,
        sourceLookup: Map<Int, SourceDTO>,
        field: String
    ): String {
        val lines = claims.joinToString("\n") { claim ->
            val src = sourceLookup[claim.sourceId]
            val value = extractFieldValue(field, claim) ?: "null"
            "- ${claim.sourceDisplayName} (level=${claim.sourceLevel}, weight=${src?.baseWeight ?: claim.sourceBaseWeight}): $value"
        }

        return """
            Multiple historical sources disagree about the $field of this church council.
            Council ID: $councilId
            Council Name: $councilName
            
            Sources:
            $lines
            
            Evaluate the sources by academic reliability and historiographical acceptance.
            Return strict JSON:
            {
              "chosen_value": "<value>",
              "reason": "<1-3 sentence justification>",
              "confidence": <0.0-1.0>
            }
        """.trimIndent()
    }

    private fun extractFieldValue(field: String, claim: SourceClaimWithMeta?): String? {
        if (claim == null) return null
        return when (field) {
            "year" -> claim.claimedYear?.toString()
            "yearEnd" -> claim.claimedYearEnd?.toString()
            "location" -> claim.claimedLocation
            "participants" -> claim.claimedParticipants?.toString()
            else -> null
        }
    }

    private suspend fun callOpenAi(prompt: String): String {
        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", "You are a historian assistant. Return JSON only."),
                ChatMessage("user", prompt)
            ),
            temperature = 0.1
        )
        val response = client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(json.encodeToString(ChatRequest.serializer(), request))
        }
        val body = response.bodyAsText()
        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("OpenAI status=${response.status} body=$body")
        }
        val parsed = json.decodeFromString(ChatResponse.serializer(), body)
        return parsed.choices.firstOrNull()?.message?.content
            ?: throw RuntimeException("OpenAI returned empty choices")
    }

    private fun parseResolution(raw: String): Resolution {
        val payload = raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val parsed = json.decodeFromString(ResolutionPayload.serializer(), payload)
            Resolution(
                chosenValue = parsed.chosenValue,
                justification = parsed.reason,
                confidence = parsed.confidence.coerceIn(0.0, 1.0)
            )
        } catch (_: Exception) {
            val chosen = Regex("chosen[_\\s]?value\"?\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1) ?: ""
            val reason = Regex("reason\"?\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1) ?: payload.take(300)
            val confidence = Regex("confidence\"?\\s*:\\s*([0-9.]+)").find(payload)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.5
            Resolution(chosenValue = chosen, justification = reason, confidence = confidence.coerceIn(0.0, 1.0))
        }
    }

    @Serializable
    private data class ResolutionPayload(
        @SerialName("chosen_value")
        val chosenValue: String,
        val reason: String,
        val confidence: Double
    )

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double
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
        val message: ChatMessage
    )
}
