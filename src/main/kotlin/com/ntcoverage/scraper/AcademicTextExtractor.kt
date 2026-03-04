package com.ntcoverage.scraper

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

@Serializable
data class ExtractedCouncilData(
    @SerialName("council_name")
    val councilName: String? = null,
    @SerialName("year_start")
    val yearStart: Int? = null,
    @SerialName("year_end")
    val yearEnd: Int? = null,
    val location: String? = null,
    @SerialName("participants_count")
    val participantsCount: Int? = null,
    @SerialName("main_topics")
    val mainTopics: String? = null,
    @SerialName("heresies_condemned")
    val heresiesCondemned: String? = null,
    @SerialName("canons_count")
    val canonsCount: Int? = null
)

class AcademicTextExtractor {
    private val log = LoggerFactory.getLogger(AcademicTextExtractor::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val apiKey: String? = System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
    private val model: String = System.getenv("OPENAI_MODEL") ?: "gpt-4o-mini"
    private val enabled: Boolean = System.getenv("ENABLE_COUNCIL_AI_EXTRACTION")?.lowercase() != "false"

    private val client = HttpClient(CIO) {
        engine { requestTimeout = 20_000 }
    }

    suspend fun extractCouncilData(text: String, sourceContext: String): ExtractedCouncilData? {
        if (text.isBlank()) return null
        if (!enabled || apiKey == null) {
            return extractHeuristic(text)
        }

        return try {
            val prompt = buildString {
                appendLine("Extract structured information about ONE Church Council from this academic text.")
                appendLine("Source: $sourceContext")
                appendLine("Return a single JSON object (NOT an array) with these keys:")
                appendLine("{ \"council_name\": \"string\", \"year_start\": number|null, \"year_end\": number|null, \"location\": \"string\"|null, \"participants_count\": number|null, \"main_topics\": \"string\"|null, \"heresies_condemned\": \"string\"|null, \"canons_count\": number|null }")
                appendLine("IMPORTANT: main_topics and heresies_condemned must be plain strings (comma-separated), NOT arrays.")
                appendLine("If the text covers multiple councils, extract only the first/main one.")
                appendLine("If uncertain about any field, return null. Do not invent data.")
            }
            val response = callOpenAi(systemPrompt = prompt, userContent = text.take(8_000))
            val normalized = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = parseRobust(normalized)
            parsed
        } catch (e: Exception) {
            log.warn("ACADEMIC_EXTRACTOR_AI_FAILED source={} error={}", sourceContext, e.message)
            extractHeuristic(text)
        }
    }

    private fun parseRobust(raw: String): ExtractedCouncilData {
        val element = json.parseToJsonElement(raw)
        val obj = when (element) {
            is JsonArray -> element.first().jsonObject
            else -> element.jsonObject
        }
        fun str(key: String): String? {
            val v = obj[key] ?: return null
            return try {
                v.jsonPrimitive.content
            } catch (_: Exception) {
                v.toString().removeSurrounding("[", "]")
                    .replace(Regex(""""\s*,\s*""""), ", ")
                    .removeSurrounding("\"")
            }
        }
        fun int(key: String): Int? = try { obj[key]?.jsonPrimitive?.content?.toIntOrNull() } catch (_: Exception) { null }

        return ExtractedCouncilData(
            councilName = str("council_name"),
            yearStart = int("year_start"),
            yearEnd = int("year_end"),
            location = str("location"),
            participantsCount = int("participants_count"),
            mainTopics = str("main_topics"),
            heresiesCondemned = str("heresies_condemned"),
            canonsCount = int("canons_count")
        )
    }

    private fun extractHeuristic(text: String): ExtractedCouncilData? {
        val year = Regex("""\b([3-9]\d{2}|10\d{2}|11\d{2})\b""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        val participants = Regex("""(\d{2,4})\s+(?:bishops|participants|fathers)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.toIntOrNull()
        val title = Regex("""(?i)(council|synod)\s+of\s+([A-Za-z\-\s]+)""")
            .find(text)?.value

        if (year == null && title == null) return null
        return ExtractedCouncilData(
            councilName = title,
            yearStart = year,
            participantsCount = participants
        )
    }

    private suspend fun callOpenAi(systemPrompt: String, userContent: String): String {
        val body = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userContent)
            ),
            temperature = 0.1
        )

        val response = client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(json.encodeToString(ChatRequest.serializer(), body))
        }
        val text = response.bodyAsText()
        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("OpenAI status=${response.status} body=$text")
        }
        val parsed = json.decodeFromString(ChatResponse.serializer(), text)
        return parsed.choices.firstOrNull()?.message?.content
            ?: throw RuntimeException("OpenAI empty choices")
    }

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
