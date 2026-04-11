package com.ntcoverage.service

import com.ntcoverage.model.*
import com.ntcoverage.repository.ApologeticResponseRepository
import com.ntcoverage.repository.ApologeticTopicRepository
import com.ntcoverage.repository.LlmQueueRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.text.Normalizer

class ApologeticsService(
    private val topicRepository: ApologeticTopicRepository,
    private val responseRepository: ApologeticResponseRepository,
    private val llmQueueRepository: LlmQueueRepository
) {
    private val log = LoggerFactory.getLogger(ApologeticsService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    // ── Reads ────────────────────────────────────────────

    fun listTopics(page: Int, limit: Int, locale: String, status: String?): ApologeticTopicsListResponse {
        log.debug("APOLOGETICS_LIST: page=$page, limit=$limit, locale=$locale, status=$status")
        val startMs = System.currentTimeMillis()
        val topics = topicRepository.findAll(page, limit, locale, status)
        val total = topicRepository.countAll(status)
        val durationMs = System.currentTimeMillis() - startMs
        log.info("APOLOGETICS_LIST: returned ${topics.size}/$total topics, page=$page, locale=$locale, status=${status ?: "ALL"}, durationMs=$durationMs")
        return ApologeticTopicsListResponse(total, topics)
    }

    fun searchTopics(query: String, limit: Int, locale: String): List<ApologeticTopicSummaryDTO> {
        log.debug("APOLOGETICS_SEARCH: query='$query', limit=$limit, locale=$locale")
        val startMs = System.currentTimeMillis()
        val results = topicRepository.search(query, limit, locale)
        val durationMs = System.currentTimeMillis() - startMs
        log.info("APOLOGETICS_SEARCH: query='$query', results=${results.size}, locale=$locale, durationMs=$durationMs")
        return results
    }

    fun getTopicDetail(slug: String, locale: String): ApologeticTopicDetailDTO? {
        log.debug("APOLOGETICS_DETAIL: slug=$slug, locale=$locale")
        val startMs = System.currentTimeMillis()
        val topic = topicRepository.findBySlug(slug, locale) ?: run {
            log.warn("APOLOGETICS_DETAIL: topic not found, slug=$slug")
            return null
        }
        val responses = responseRepository.findByTopicId(topic.id, locale)
        val durationMs = System.currentTimeMillis() - startMs
        log.info("APOLOGETICS_DETAIL: slug=$slug, responseCount=${responses.size}, locale=$locale, durationMs=$durationMs")
        return topic.copy(responses = responses)
    }

    fun getTopicById(id: Int, locale: String): ApologeticTopicDetailDTO? {
        log.debug("APOLOGETICS_DETAIL_BY_ID: id=$id, locale=$locale")
        val topic = topicRepository.findById(id, locale) ?: run {
            log.warn("APOLOGETICS_DETAIL_BY_ID: topic not found, id=$id")
            return null
        }
        val responses = responseRepository.findByTopicId(topic.id, locale)
        return topic.copy(responses = responses)
    }

    // ── Topic creation (enqueues LLM generation via queue) ──

    fun createTopic(prompt: String, createdByEmail: String?): ApologeticTopicDetailDTO {
        val promptWords = wordCount(prompt)
        log.info("APOLOGETICS_CREATE_TOPIC: starting, promptWords=$promptWords, by=$createdByEmail")
        val startMs = System.currentTimeMillis()

        // Step 1: Slug from prompt (stable — won't change after LLM processes)
        val slug = generateSlug(prompt.take(200))
        log.debug("APOLOGETICS_CREATE_TOPIC: generated slug='$slug'")

        // Step 2: DB insert with placeholder title/body and PROCESSING status
        val placeholderTitle = prompt.take(100).let { if (prompt.length > 100) "$it..." else it }
        val id = topicRepository.insert(
            title = placeholderTitle,
            slug = slug,
            originalPrompt = prompt,
            body = "Aguardando processamento pela IA...",
            createdByEmail = createdByEmail,
            status = "PROCESSING"
        )
        log.info("APOLOGETICS_CREATE_TOPIC: db_saved, id=$id, slug=$slug, status=PROCESSING")

        // Step 3: Enqueue LLM generation
        val systemPrompt = """You are an expert in biblical apologetics and hermeneutics.
            |The user will describe a skeptic/atheist argument against the Bible.
            |Generate a JSON response with:
            |1. "title": A concise, descriptive title for this argument (max 100 chars, in Portuguese)
            |2. "body": A well-structured summary of the argument (3-5 paragraphs, academic tone, include relevant Bible references, in Portuguese).
            |Present the argument fairly and accurately as a skeptic would state it.
            |Return ONLY valid JSON: {"title":"...","body":"..."}""".trimMargin()

        val queueId = llmQueueRepository.enqueue(
            phaseName = "apologetics_generate_topic",
            label = "APOLOGETICS_TOPIC_$id",
            systemPrompt = systemPrompt,
            userContent = prompt,
            temperature = 0.3,
            maxTokens = 2000,
            tier = "HIGH",
            callbackContext = """{"topicId":$id}"""
        )
        log.info("APOLOGETICS_CREATE_TOPIC: enqueued, topicId=$id, queueId=$queueId, tier=HIGH")

        val durationMs = System.currentTimeMillis() - startMs
        log.info("APOLOGETICS_CREATE_TOPIC: completed, id=$id, slug=$slug, promptWords=$promptWords, durationMs=$durationMs, by=$createdByEmail")

        return topicRepository.findById(id, "en")!!
    }

    // ── Response creation (user writes, LLM optionally complements via queue) ──

    fun createResponse(topicId: Int, body: String, useAi: Boolean, createdByEmail: String?): ApologeticResponseDTO {
        val bodyWords = wordCount(body)
        log.info("APOLOGETICS_CREATE_RESPONSE: starting, topicId=$topicId, useAi=$useAi, bodyWords=$bodyWords, by=$createdByEmail")
        val startMs = System.currentTimeMillis()

        // Save response immediately with user's original body
        val id = responseRepository.insert(topicId, body, body, createdByEmail)
        log.info("APOLOGETICS_CREATE_RESPONSE: db_saved, id=$id, topicId=$topicId")

        // Enqueue AI complement if requested
        if (useAi) {
            val topic = topicRepository.findById(topicId, "en")
                ?: throw NoSuchElementException("Topic not found: $topicId")
            log.info("APOLOGETICS_CREATE_RESPONSE: enqueuing ai_complement, topicTitle='${topic.title.take(60)}'")

            val systemPrompt = """You are a Christian apologist and biblical scholar.
                |The user has written their own apologetic response to a skeptic argument.
                |Your job is to COMPLEMENT and ENRICH their response — do NOT replace it.
                |- Preserve the user's original arguments and voice
                |- Add relevant Scripture references if missing
                |- Add historical evidence or scholarly sources that support their points
                |- Improve structure and clarity if needed
                |- If the user explicitly asks you to generate a full response, then do so
                |Write in Portuguese. Return the enhanced response text only.""".trimMargin()

            val userContent = """Argumento cético:
                |Título: ${topic.title}
                |${topic.body}
                |
                |Resposta do usuário:
                |$body""".trimMargin()

            val queueId = llmQueueRepository.enqueue(
                phaseName = "apologetics_complement_response",
                label = "APOLOGETICS_RESPONSE_$id",
                systemPrompt = systemPrompt,
                userContent = userContent,
                temperature = 0.4,
                maxTokens = 3000,
                tier = "HIGH",
                callbackContext = """{"responseId":$id,"topicId":$topicId}"""
            )
            log.info("APOLOGETICS_CREATE_RESPONSE: enqueued, responseId=$id, queueId=$queueId, tier=HIGH")
        }

        val durationMs = System.currentTimeMillis() - startMs
        log.info("APOLOGETICS_CREATE_RESPONSE: completed, id=$id, topicId=$topicId, useAi=$useAi, durationMs=$durationMs, by=$createdByEmail")

        val responses = responseRepository.findByTopicId(topicId, "en")
        return responses.first { it.id == id }
    }

    // ── Updates ──────────────────────────────────────────

    fun updateTopic(id: Int, request: UpdateApologeticTopicRequest): Boolean {
        log.info("APOLOGETICS_UPDATE_TOPIC: id=$id, hasTitle=${request.title != null}, hasBody=${request.body != null}, hasStatus=${request.status != null}, hasReviewed=${request.bodyReviewed != null}")
        val startMs = System.currentTimeMillis()
        val updated = topicRepository.update(id, request.title, request.body, request.status, request.bodyReviewed)
        val durationMs = System.currentTimeMillis() - startMs
        if (updated) {
            log.info("APOLOGETICS_UPDATE_TOPIC: success, id=$id, newStatus=${request.status ?: "unchanged"}, durationMs=$durationMs")
        } else {
            log.warn("APOLOGETICS_UPDATE_TOPIC: not found, id=$id")
        }
        return updated
    }

    fun updateResponse(id: Int, request: UpdateApologeticResponseRequest): Boolean {
        log.info("APOLOGETICS_UPDATE_RESPONSE: id=$id, hasBody=${request.body != null}, useAi=${request.useAi}, hasReviewed=${request.bodyReviewed != null}")
        val startMs = System.currentTimeMillis()

        // Save body immediately
        val updated = responseRepository.update(id, request.body, request.bodyReviewed)
        if (!updated) {
            log.warn("APOLOGETICS_UPDATE_RESPONSE: not found, id=$id")
            return false
        }

        // Enqueue AI enrichment if requested
        if (request.useAi == true && request.body != null) {
            log.info("APOLOGETICS_UPDATE_RESPONSE: enqueuing ai_complement for response id=$id, bodyWords=${wordCount(request.body)}")
            val queueId = llmQueueRepository.enqueue(
                phaseName = "apologetics_complement_response",
                label = "APOLOGETICS_RESPONSE_$id",
                systemPrompt = "You are a biblical scholar. Enrich this apologetic response with Scripture references, historical evidence, and scholarly sources. Preserve the original voice. Write in Portuguese. Return enhanced text only.",
                userContent = request.body,
                temperature = 0.4,
                maxTokens = 3000,
                tier = "HIGH",
                callbackContext = """{"responseId":$id}"""
            )
            log.info("APOLOGETICS_UPDATE_RESPONSE: enqueued, responseId=$id, queueId=$queueId, tier=HIGH")
        }

        val durationMs = System.currentTimeMillis() - startMs
        log.info("APOLOGETICS_UPDATE_RESPONSE: success, id=$id, durationMs=$durationMs")
        return true
    }

    fun deleteResponse(id: Int): Boolean {
        log.info("APOLOGETICS_DELETE_RESPONSE: id=$id")
        val deleted = responseRepository.delete(id)
        if (deleted) {
            log.info("APOLOGETICS_DELETE_RESPONSE: success, id=$id")
        } else {
            log.warn("APOLOGETICS_DELETE_RESPONSE: not found, id=$id")
        }
        return deleted
    }

    companion object {
        private val parseJson = Json { ignoreUnknownKeys = true }

        fun parseTopicResult(content: String): Pair<String, String> {
            return try {
                val clean = content.trim()
                    .removeSurrounding("```json", "```")
                    .removeSurrounding("```", "```")
                    .trim()
                val obj = parseJson.parseToJsonElement(clean).jsonObject
                val title = obj["title"]?.jsonPrimitive?.content ?: "Untitled"
                val body = obj["body"]?.jsonPrimitive?.content ?: content
                title to body
            } catch (_: Exception) {
                val titleMatch = Regex(""""title"\s*:\s*"([^"]+)"""").find(content)
                val title = titleMatch?.groupValues?.get(1) ?: "Untitled"
                title to content
            }
        }

        fun generateSlug(title: String): String {
            val normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
                .lowercase()
                .replace(Regex("[^a-z0-9\\s-]"), "")
                .trim()
                .replace(Regex("\\s+"), "-")
                .take(200)
            return normalized.ifBlank { "topic" }
        }

        private fun wordCount(text: String): Int = text.split("\\s+".toRegex()).size
    }
}
