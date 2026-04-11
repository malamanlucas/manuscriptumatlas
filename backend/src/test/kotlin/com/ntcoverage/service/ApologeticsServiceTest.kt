package com.ntcoverage.service

import com.ntcoverage.model.*
import com.ntcoverage.repository.ApologeticResponseRepository
import com.ntcoverage.repository.ApologeticTopicRepository
import com.ntcoverage.repository.LlmQueueRepository
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApologeticsServiceTest {

    private val topicRepository = mockk<ApologeticTopicRepository>()
    private val responseRepository = mockk<ApologeticResponseRepository>()
    private val llmQueueRepository = mockk<LlmQueueRepository>()

    private val service = ApologeticsService(
        topicRepository = topicRepository,
        responseRepository = responseRepository,
        llmQueueRepository = llmQueueRepository
    )

    // ── Slug generation ──────────────────────────────────

    @Test
    fun `generateSlug creates slug from simple title`() {
        val slug = ApologeticsService.generateSlug("Jesus Had Two Fathers")
        assertEquals("jesus-had-two-fathers", slug)
    }

    @Test
    fun `generateSlug removes accents`() {
        val slug = ApologeticsService.generateSlug("A contradição dos pais de José")
        assertEquals("a-contradicao-dos-pais-de-jose", slug)
    }

    @Test
    fun `generateSlug removes special characters`() {
        val slug = ApologeticsService.generateSlug("Does God exist? A philosophical argument!")
        assertEquals("does-god-exist-a-philosophical-argument", slug)
    }

    @Test
    fun `generateSlug collapses multiple spaces`() {
        val slug = ApologeticsService.generateSlug("Too   many   spaces")
        assertEquals("too-many-spaces", slug)
    }

    @Test
    fun `generateSlug truncates long titles`() {
        val longTitle = "A".repeat(300)
        val slug = ApologeticsService.generateSlug(longTitle)
        assertTrue(slug.length <= 200)
    }

    @Test
    fun `generateSlug handles empty string`() {
        val slug = ApologeticsService.generateSlug("")
        assertEquals("topic", slug)
    }

    @Test
    fun `generateSlug handles only special chars`() {
        val slug = ApologeticsService.generateSlug("!@#\$%^&*()")
        assertEquals("topic", slug)
    }

    // ── parseTopicResult ─────────────────────────────────

    @Test
    fun `parseTopicResult parses valid JSON`() {
        val json = """{"title": "Dois pais de José", "body": "O argumento cético alega..."}"""
        val (title, body) = ApologeticsService.parseTopicResult(json)
        assertEquals("Dois pais de José", title)
        assertEquals("O argumento cético alega...", body)
    }

    @Test
    fun `parseTopicResult handles malformed JSON with fallback`() {
        val malformed = """Sure! Here's the topic: {"title": "Bad JSON", "body": "Some text"""
        val (title, _) = ApologeticsService.parseTopicResult(malformed)
        assertEquals("Bad JSON", title)
    }

    @Test
    fun `parseTopicResult strips code fences`() {
        val fenced = "```json\n{\"title\": \"Test Title\", \"body\": \"Test body\"}\n```"
        val (title, body) = ApologeticsService.parseTopicResult(fenced)
        assertEquals("Test Title", title)
        assertEquals("Test body", body)
    }

    // ── Topic listing ────────────────────────────────────

    @Test
    fun `listTopics returns paginated response`() {
        val topics = listOf(
            ApologeticTopicSummaryDTO(1, "Title 1", "title-1", "DRAFT", 0, "2026-01-01T00:00:00Z"),
            ApologeticTopicSummaryDTO(2, "Title 2", "title-2", "PUBLISHED", 2, "2026-01-02T00:00:00Z")
        )
        every { topicRepository.findAll(1, 20, "en", null) } returns topics
        every { topicRepository.countAll(null) } returns 2

        val result = service.listTopics(1, 20, "en", null)

        assertEquals(2, result.total)
        assertEquals(2, result.topics.size)
        assertEquals("Title 1", result.topics[0].title)
        verify { topicRepository.findAll(1, 20, "en", null) }
        verify { topicRepository.countAll(null) }
    }

    @Test
    fun `listTopics filters by status`() {
        every { topicRepository.findAll(1, 20, "pt", "DRAFT") } returns emptyList()
        every { topicRepository.countAll("DRAFT") } returns 0

        val result = service.listTopics(1, 20, "pt", "DRAFT")

        assertEquals(0, result.total)
        assertEquals(0, result.topics.size)
        verify { topicRepository.findAll(1, 20, "pt", "DRAFT") }
    }

    // ── Search ───────────────────────────────────────────

    @Test
    fun `searchTopics delegates to repository`() {
        val expected = listOf(
            ApologeticTopicSummaryDTO(1, "José", "jose", "PUBLISHED", 1, "2026-01-01T00:00:00Z")
        )
        every { topicRepository.search("José", 20, "pt") } returns expected

        val result = service.searchTopics("José", 20, "pt")

        assertEquals(1, result.size)
        assertEquals("José", result[0].title)
    }

    // ── Topic detail ────────────────────────────────────

    @Test
    fun `getTopicDetail returns null when not found`() {
        every { topicRepository.findBySlug("nonexistent", "en") } returns null

        val result = service.getTopicDetail("nonexistent", "en")

        assertEquals(null, result)
    }

    @Test
    fun `getTopicDetail includes responses`() {
        val topic = ApologeticTopicDetailDTO(
            id = 1, title = "Test", slug = "test", originalPrompt = "prompt",
            body = "body", status = "DRAFT", createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        val responses = listOf(
            ApologeticResponseDTO(1, 1, "my response", "enhanced body", false, 1, "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
        )
        every { topicRepository.findBySlug("test", "en") } returns topic
        every { responseRepository.findByTopicId(1, "en") } returns responses

        val result = service.getTopicDetail("test", "en")

        assertNotNull(result)
        assertEquals(1, result.responses.size)
        assertEquals("enhanced body", result.responses[0].body)
    }

    // ── Topic creation (queue-based) ─────────────────────

    @Test
    fun `createTopic saves with PROCESSING status and enqueues`() {
        every { topicRepository.insert(any(), any(), "meu prompt", "Aguardando processamento pela IA...", "user@test.com", "PROCESSING") } returns 1
        every { llmQueueRepository.enqueue(
            phaseName = "apologetics_generate_topic",
            label = "APOLOGETICS_TOPIC_1",
            systemPrompt = any(),
            userContent = "meu prompt",
            temperature = 0.3,
            maxTokens = 2000,
            tier = "HIGH",
            callbackContext = """{"topicId":1}"""
        ) } returns 100
        every { topicRepository.findById(1, "en") } returns ApologeticTopicDetailDTO(
            id = 1, title = "meu prompt", slug = "meu-prompt",
            originalPrompt = "meu prompt", body = "Aguardando processamento pela IA...",
            status = "PROCESSING", createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z"
        )

        val result = service.createTopic("meu prompt", "user@test.com")

        assertEquals("PROCESSING", result.status)
        assertEquals("meu prompt", result.originalPrompt)
        verify { topicRepository.insert(any(), any(), "meu prompt", "Aguardando processamento pela IA...", "user@test.com", "PROCESSING") }
        verify { llmQueueRepository.enqueue(
            phaseName = "apologetics_generate_topic",
            label = "APOLOGETICS_TOPIC_1",
            systemPrompt = any(),
            userContent = "meu prompt",
            temperature = 0.3,
            maxTokens = 2000,
            tier = "HIGH",
            callbackContext = """{"topicId":1}"""
        ) }
    }

    // ── Response creation ────────────────────────────────

    @Test
    fun `createResponse without AI saves user text as-is`() {
        val userBody = "Minha resposta apologética"
        every { responseRepository.insert(1, userBody, userBody, "user@test.com") } returns 10
        every { responseRepository.findByTopicId(1, "en") } returns listOf(
            ApologeticResponseDTO(10, 1, userBody, userBody, false, 1, "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
        )

        val result = service.createResponse(1, userBody, false, "user@test.com")

        assertEquals(userBody, result.body)
        verify(exactly = 0) { llmQueueRepository.enqueue(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `createResponse with AI saves immediately and enqueues complement`() {
        val userBody = "Jesus tinha apenas um pai biológico"

        every { responseRepository.insert(1, userBody, userBody, "user@test.com") } returns 11
        every { topicRepository.findById(1, "en") } returns ApologeticTopicDetailDTO(
            id = 1, title = "Dois pais", slug = "dois-pais", originalPrompt = "prompt",
            body = "O argumento...", status = "DRAFT",
            createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-01T00:00:00Z"
        )
        every { llmQueueRepository.enqueue(
            phaseName = "apologetics_complement_response",
            label = "APOLOGETICS_RESPONSE_11",
            systemPrompt = any(),
            userContent = any(),
            temperature = 0.4,
            maxTokens = 3000,
            tier = "HIGH",
            callbackContext = """{"responseId":11,"topicId":1}"""
        ) } returns 200
        every { responseRepository.findByTopicId(1, "en") } returns listOf(
            ApologeticResponseDTO(11, 1, userBody, userBody, false, 1, "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")
        )

        val result = service.createResponse(1, userBody, true, "user@test.com")

        assertEquals(userBody, result.body)
        verify { llmQueueRepository.enqueue(
            phaseName = "apologetics_complement_response",
            label = "APOLOGETICS_RESPONSE_11",
            systemPrompt = any(),
            userContent = any(),
            temperature = 0.4,
            maxTokens = 3000,
            tier = "HIGH",
            callbackContext = """{"responseId":11,"topicId":1}"""
        ) }
    }

    // ── Update / Delete ──────────────────────────────────

    @Test
    fun `updateTopic delegates to repository`() {
        every { topicRepository.update(1, "New Title", null, "PUBLISHED", null) } returns true

        val result = service.updateTopic(1, UpdateApologeticTopicRequest(title = "New Title", status = "PUBLISHED"))

        assertTrue(result)
        verify { topicRepository.update(1, "New Title", null, "PUBLISHED", null) }
    }

    @Test
    fun `deleteResponse delegates to repository`() {
        every { responseRepository.delete(5) } returns true

        val result = service.deleteResponse(5)

        assertTrue(result)
        verify { responseRepository.delete(5) }
    }
}
