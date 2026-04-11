package com.ntcoverage.service

import com.ntcoverage.model.PhaseStatusDTO
import com.ntcoverage.repository.IngestionPhaseRepository
import org.slf4j.LoggerFactory

class IngestionPhaseTracker(
    private val phaseRepository: IngestionPhaseRepository
) {
    private val log = LoggerFactory.getLogger(IngestionPhaseTracker::class.java)

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

    fun getPhasesByPrefix(prefix: String): List<PhaseStatusDTO> = phaseRepository.getByPrefix(prefix)

    fun deleteByPrefix(prefix: String): Int = phaseRepository.deleteByPrefix(prefix)

    fun isPhaseCompleted(phase: String): Boolean {
        val status = phaseRepository.getByPhase(phase)
        return status?.status == "success"
    }

    fun isAnyRunning(): Boolean = phaseRepository.isAnyRunning()

    fun isAnyRunningByPrefix(prefix: String): Boolean = phaseRepository.isAnyRunningByPrefix(prefix)

    fun isPhaseRunning(phaseName: String): Boolean = phaseRepository.isPhaseRunning(phaseName)
}
