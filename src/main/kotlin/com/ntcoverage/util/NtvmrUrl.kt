package com.ntcoverage.util

object NtvmrUrl {
    private const val BASE = "https://ntvmr.uni-muenster.de/manuscript-workspace"

    fun gaIdToDocId(gaId: String): String {
        val papyrusMatch = Regex("""^P(\d+)$""").matchEntire(gaId)
        if (papyrusMatch != null) {
            val num = papyrusMatch.groupValues[1].toInt()
            return "1${num.toString().padStart(4, '0')}"
        }
        val uncialMatch = Regex("""^0?(\d+)$""").matchEntire(gaId)
        if (uncialMatch != null) {
            val num = uncialMatch.groupValues[1].toInt()
            return "2${num.toString().padStart(4, '0')}"
        }
        return gaId
    }

    fun buildUrl(gaId: String): String = "$BASE?docID=${gaIdToDocId(gaId)}"
}
