package com.ntcoverage.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class BiographySummarizationService {

    private val log = LoggerFactory.getLogger(BiographySummarizationService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val apiKey: String? = System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
    private val model: String = System.getenv("OPENAI_MODEL") ?: "gpt-4o-mini"
    private val timeoutMs: Long = System.getenv("OPENAI_TIMEOUT_MS")?.toLongOrNull() ?: 12_000L
    private val summarizationEnabled: Boolean = System.getenv("ENABLE_BIO_SUMMARIZATION")?.lowercase() != "false"
    private val translationEnabled: Boolean = System.getenv("ENABLE_BIO_TRANSLATION")?.lowercase() != "false"

    private val localeToLanguage = mapOf("pt" to "Portuguese", "es" to "Spanish")

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = timeoutMs
        }
    }

    suspend fun summarizeIfNeeded(original: String?): String? {
        if (original == null) return null
        if (!isLong(original)) return original

        if (!summarizationEnabled) {
            log.info("BIO_SUMMARIZATION disabled — returning original for text with ${wordCount(original)} words")
            return original
        }
        if (apiKey == null) {
            log.warn("BIO_SUMMARIZATION: OPENAI_API_KEY not set — returning original")
            return original
        }

        return callWithRetry(
            systemPrompt = "Summarize the following Church Father biography in a neutral academic tone, " +
                "5-8 sentences, preserving historical precision. Do not introduce theological judgments.",
            userContent = original,
            fallback = original,
            logPrefix = "BIO_SUMMARIZATION"
        )
    }

    suspend fun translateBiography(text: String, targetLocale: String, fatherName: String = ""): String? {
        if (!translationEnabled) {
            log.info("BIO_TRANSLATION disabled — skipping father=$fatherName locale=$targetLocale")
            return null
        }
        if (apiKey == null) {
            log.warn("BIO_TRANSLATION: OPENAI_API_KEY not set — skipping father=$fatherName locale=$targetLocale")
            return null
        }

        val language = localeToLanguage[targetLocale]
        if (language == null) {
            log.warn("BIO_TRANSLATION: unsupported locale=$targetLocale — skipping")
            return null
        }

        val systemPrompt = "Translate faithfully the following Church Father biography to $language. " +
            "Do not interpret, summarize, expand, or omit content. " +
            "Preserve all proper names, dates, and historical references exactly. " +
            "Maintain neutral academic tone."

        val startMs = System.currentTimeMillis()
        val result = callWithRetry(
            systemPrompt = systemPrompt,
            userContent = text,
            fallback = null,
            logPrefix = "BIO_TRANSLATION"
        )
        val durationMs = System.currentTimeMillis() - startMs

        if (result != null) {
            log.info("BIO_TRANSLATION: father=$fatherName, locale=$targetLocale, words=${wordCount(text)}->${wordCount(result)}, durationMs=$durationMs, status=success")
        } else {
            log.error("BIO_TRANSLATION: father=$fatherName, locale=$targetLocale, durationMs=$durationMs, status=failed")
        }

        return result
    }

    private suspend fun callWithRetry(systemPrompt: String, userContent: String, fallback: String?, logPrefix: String): String? {
        for (attempt in 1..2) {
            try {
                return callOpenAi(systemPrompt, userContent)
            } catch (e: Exception) {
                if (attempt < 2) {
                    log.warn("$logPrefix attempt $attempt failed, retrying: ${e.message}")
                } else {
                    log.error("$logPrefix failed after 2 attempts: ${e.message}")
                }
            }
        }
        return fallback
    }

    private suspend fun callOpenAi(systemPrompt: String, userContent: String): String {
        val requestBody = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userContent)
            ),
            temperature = 0.3
        )

        val response = client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(json.encodeToString(ChatRequest.serializer(), requestBody))
        }

        val body = response.bodyAsText()
        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("OpenAI returned ${response.status}: $body")
        }

        val chatResponse = json.decodeFromString<ChatResponse>(body)
        return chatResponse.choices.firstOrNull()?.message?.content?.trim()
            ?: throw RuntimeException("OpenAI returned empty choices")
    }

    fun close() {
        client.close()
    }

    companion object {
        private fun wordCount(text: String): Int = text.split("\\s+".toRegex()).size

        private fun isLong(text: String): Boolean = wordCount(text) > 120

        fun isLongBiography(text: String?): Boolean =
            text != null && isLong(text)
    }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.3
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
}
