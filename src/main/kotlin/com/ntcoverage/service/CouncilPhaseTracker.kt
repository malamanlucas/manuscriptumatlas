package com.ntcoverage.service

import com.ntcoverage.model.PhaseStatusDTO
import com.ntcoverage.repository.CouncilIngestionPhaseRepository
import org.slf4j.LoggerFactory

class CouncilPhaseTracker(
    private val phaseRepository: CouncilIngestionPhaseRepository
) {
    private val log = LoggerFactory.getLogger(CouncilPhaseTracker::class.java)

    fun recoverStuckPhases() {
        val stuck = phaseRepository.getAll().filter { it.status == "running" }
        if (stuck.isNotEmpty()) {
            log.warn("PHASE_TRACKER: recovering {} stuck phases from previous run: {}", stuck.size, stuck.map { it.phaseName })
            stuck.forEach { phase ->
                phaseRepository.markFailed(phaseName = phase.phaseName, error = "Interrupted by server restart")
            }
        }
    }

    fun markRunning(phase: String, total: Int = 0, runBy: String = "manual"): PhaseStatusDTO =
        phaseRepository.markRunning(phaseName = phase, itemsTotal = total, lastRunBy = runBy)

    fun markProgress(phase: String, processed: Int, total: Int? = null): Boolean =
        phaseRepository.markProgress(phaseName = phase, processed = processed, total = total)

    fun markSuccess(phase: String, processed: Int): Boolean =
        phaseRepository.markSuccess(phaseName = phase, processed = processed)

    fun markFailed(phase: String, error: String): Boolean =
        phaseRepository.markFailed(phaseName = phase, error = error)

    fun getPhaseStatus(phase: String): PhaseStatusDTO? = phaseRepository.getByPhase(phase)

    fun getAllPhases(): List<PhaseStatusDTO> = phaseRepository.getAll()

    fun isAnyRunning(): Boolean = phaseRepository.isAnyRunning()
}
