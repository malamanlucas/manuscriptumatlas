package com.ntcoverage.service

import com.ntcoverage.model.SourceDTO
import com.ntcoverage.repository.LlmQueueRepository
import com.ntcoverage.repository.SourceClaimWithMeta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ConflictResolutionService(
    private val llmQueueRepository: LlmQueueRepository? = null
) {
    data class Resolution(
        val chosenValue: String,
        val justification: String,
        val confidence: Double
    )

    private val log = LoggerFactory.getLogger(ConflictResolutionService::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    private val enabled = System.getenv("ENABLE_COUNCIL_CONFLICT_AI")?.lowercase() != "false"

    fun resolveConflictSync(
        councilId: Int,
        claims: List<SourceClaimWithMeta>,
        sources: List<SourceDTO>,
        field: String,
        councilName: String = "Unknown council"
    ): Resolution {
        if (claims.isEmpty()) {
            return Resolution(chosenValue = "", justification = "No claims available", confidence = 0.0)
        }

        val fallback = claims.maxByOrNull { it.sourceBaseWeight }
        return Resolution(
            chosenValue = extractFieldValue(field, fallback) ?: "",
            justification = "Used highest weighted source (LLM via queue for detailed resolution).",
            confidence = 0.55
        )
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

    fun enqueueConflict(
        councilId: Int,
        claims: List<SourceClaimWithMeta>,
        sources: List<SourceDTO>,
        field: String,
        councilName: String = "Unknown council"
    ): Int? {
        val repo = llmQueueRepository ?: return null
        if (claims.isEmpty()) return null

        val sourceLookup = sources.associateBy { it.id }
        val prompt = buildPrompt(councilId, councilName, claims, sourceLookup, field)
        val callbackContext = json.encodeToString(
            ConflictContext.serializer(),
            ConflictContext(councilId = councilId, councilName = councilName, field = field)
        )

        return repo.enqueue(
            phaseName = "council_consensus",
            label = "ConflictResolution:${councilId}:${field}",
            systemPrompt = "You are a historian assistant. Return JSON only.",
            userContent = prompt,
            temperature = 0.1,
            maxTokens = 500,
            tier = "MEDIUM",
            callbackContext = callbackContext
        )
    }

    @Serializable
    private data class ConflictContext(
        val councilId: Int,
        val councilName: String,
        val field: String
    )

    @Serializable
    private data class ResolutionPayload(
        @SerialName("chosen_value")
        val chosenValue: String,
        val reason: String,
        val confidence: Double
    )
}
