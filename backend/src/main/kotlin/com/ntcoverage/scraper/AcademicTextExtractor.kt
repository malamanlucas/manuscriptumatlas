package com.ntcoverage.scraper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

    fun extractCouncilData(text: String, sourceContext: String): ExtractedCouncilData? {
        if (text.isBlank()) return null
        return extractHeuristic(text)
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
}
