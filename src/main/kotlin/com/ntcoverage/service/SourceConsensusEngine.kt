package com.ntcoverage.service

import com.ntcoverage.repository.CouncilSourceClaimRepository
import com.ntcoverage.repository.SourceClaimWithMeta
import com.ntcoverage.repository.SourceRepository
import org.slf4j.LoggerFactory

class SourceConsensusEngine(
    private val sourceRepository: SourceRepository,
    private val claimRepository: CouncilSourceClaimRepository,
    private val conflictResolver: ConflictResolutionService
) {
    data class ConsensusResult(
        val consensusYear: Int? = null,
        val consensusYearEnd: Int? = null,
        val consensusLocation: String? = null,
        val consensusParticipants: Int? = null,
        val confidenceScore: Double,
        val dataConfidence: String,
        val sourceCount: Int,
        val conflictResolution: String? = null
    )

    private val log = LoggerFactory.getLogger(SourceConsensusEngine::class.java)

    suspend fun calculateConsensus(councilId: Int, councilName: String = "Unknown council"): ConsensusResult {
        val claims = claimRepository.findByCouncil(councilId)
        if (claims.isEmpty()) {
            return ConsensusResult(confidenceScore = 0.0, dataConfidence = "LOW", sourceCount = 0)
        }

        val yearConsensus = calculateIntConsensus(claims.mapNotNull { claim ->
            claim.claimedYear?.let { it to claim.sourceBaseWeight }
        })
        val yearEndConsensus = calculateIntConsensus(claims.mapNotNull { claim ->
            claim.claimedYearEnd?.let { it to claim.sourceBaseWeight }
        })
        val locationConsensus = calculateStringConsensus(claims.mapNotNull { claim ->
            claim.claimedLocation?.let { it to claim.sourceBaseWeight }
        })
        val participantsConsensus = calculateIntConsensus(claims.mapNotNull { claim ->
            claim.claimedParticipants?.let { it to claim.sourceBaseWeight }
        })

        val baseConfidence = listOf(
            yearConsensus?.confidence ?: 0.0,
            locationConsensus?.confidence ?: 0.0,
            participantsConsensus?.confidence ?: 0.0
        ).average()

        var chosenYear = yearConsensus?.value
        var conflictText: String? = null
        var finalConfidence = baseConfidence

        if ((yearConsensus?.confidence ?: 0.0) < 0.70 && claims.size >= 2) {
            val resolution = conflictResolver.resolveConflict(
                councilId = councilId,
                claims = claims,
                sources = sourceRepository.findAll(),
                field = "year",
                councilName = councilName
            )
            chosenYear = resolution.chosenValue.toIntOrNull() ?: chosenYear
            conflictText = resolution.justification
            finalConfidence = ((finalConfidence + resolution.confidence) / 2.0).coerceIn(0.0, 1.0)
        }

        val dataConfidence = when {
            finalConfidence >= 0.85 -> "HIGH"
            finalConfidence >= 0.60 -> "MEDIUM"
            else -> "LOW"
        }

        return ConsensusResult(
            consensusYear = chosenYear,
            consensusYearEnd = yearEndConsensus?.value,
            consensusLocation = locationConsensus?.value,
            consensusParticipants = participantsConsensus?.value,
            confidenceScore = finalConfidence,
            dataConfidence = dataConfidence,
            sourceCount = claims.size,
            conflictResolution = conflictText
        )
    }

    fun updateSourceReliability(sourceId: Int): Boolean {
        val claims = claimRepository.findBySource(sourceId)
        if (claims.isEmpty()) return false

        val grouped = claims.groupBy { it.councilId }
        var total = 0
        var agrees = 0

        for ((_, councilClaims) in grouped) {
            val consensus = calculateIntConsensus(councilClaims.mapNotNull {
                it.claimedYear?.let { year -> year to it.sourceBaseWeight }
            }) ?: continue
            for (claim in councilClaims) {
                if (claim.sourceId != sourceId) continue
                total++
                if (claim.claimedYear == consensus.value) agrees++
            }
        }

        if (total == 0) return false
        val reliability = agrees.toDouble() / total.toDouble()
        val updated = sourceRepository.updateReliability(sourceId, reliability)
        if (updated) {
            log.info("SOURCE_RELIABILITY_UPDATED sourceId={} reliability={}", sourceId, reliability)
        }
        return updated
    }

    private fun calculateIntConsensus(values: List<Pair<Int, Double>>): FieldConsensus<Int>? {
        if (values.isEmpty()) return null
        val grouped = values.groupBy { it.first }
            .mapValues { (_, pairs) -> pairs.sumOf { it.second } }
        val totalWeight = grouped.values.sum().takeIf { it > 0.0 } ?: return null
        val best = grouped.maxByOrNull { it.value } ?: return null
        return FieldConsensus(value = best.key, confidence = best.value / totalWeight)
    }

    private fun calculateStringConsensus(values: List<Pair<String, Double>>): FieldConsensus<String>? {
        if (values.isEmpty()) return null
        val grouped = values.groupBy { it.first.trim().lowercase() }
            .mapValues { (_, pairs) -> pairs.sumOf { it.second } }
        val totalWeight = grouped.values.sum().takeIf { it > 0.0 } ?: return null
        val best = grouped.maxByOrNull { it.value } ?: return null
        return FieldConsensus(value = best.key, confidence = best.value / totalWeight)
    }

    private data class FieldConsensus<T>(
        val value: T,
        val confidence: Double
    )
}
