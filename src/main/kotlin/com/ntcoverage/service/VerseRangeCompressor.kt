package com.ntcoverage.service

import com.ntcoverage.seed.CanonicalVerses

/**
 * Compresses a sorted list of (chapter, verse) pairs into human-readable ranges.
 * e.g. [(18,31), (18,32), (18,33), (18,37), (18,38)] -> ["18:31-33", "18:37-38"]
 */
object VerseRangeCompressor {

    fun compress(bookName: String, verses: List<Pair<Int, Int>>): List<String> {
        if (verses.isEmpty()) return emptyList()
        val sorted = verses.sortedWith(compareBy({ it.first }, { it.second }))
        val ranges = mutableListOf<String>()
        var startCh = sorted[0].first
        var startV = sorted[0].second
        var endCh = startCh
        var endV = startV

        for (i in 1 until sorted.size) {
            val (ch, v) = sorted[i]
            val isConsecutive = when {
                ch == endCh && v == endV + 1 -> true
                ch == endCh + 1 && v == 1 -> {
                    try {
                        val prevMax = CanonicalVerses.versesPerChapter(bookName, endCh)
                        endV == prevMax
                    } catch (_: Exception) { false }
                }
                else -> false
            }
            if (isConsecutive) {
                endCh = ch
                endV = v
            } else {
                ranges.add(formatRange(startCh, startV, endCh, endV))
                startCh = ch
                startV = v
                endCh = ch
                endV = v
            }
        }
        ranges.add(formatRange(startCh, startV, endCh, endV))
        return ranges
    }

    private fun formatRange(startCh: Int, startV: Int, endCh: Int, endV: Int): String {
        return if (startCh == endCh && startV == endV) {
            "$startCh:$startV"
        } else if (startCh == endCh) {
            "$startCh:$startV-$endV"
        } else {
            "$startCh:$startV-$endCh:$endV"
        }
    }
}
