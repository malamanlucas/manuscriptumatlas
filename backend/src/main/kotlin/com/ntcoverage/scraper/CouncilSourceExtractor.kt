package com.ntcoverage.scraper

interface CouncilSourceExtractor {
    val sourceName: String

    suspend fun extract(): List<ExtractedCouncilClaim>
}

data class ExtractedCouncilClaim(
    val councilNameRaw: String,
    val normalizedKey: String,
    val claimedYear: Int? = null,
    val claimedYearEnd: Int? = null,
    val claimedLocation: String? = null,
    val claimedParticipants: Int? = null,
    val rawText: String? = null,
    val sourcePage: String? = null
)
