package com.ntcoverage.service

import com.ntcoverage.seed.CanonicalVerses

data class SingleVerse(
    val bookName: String,
    val chapter: Int,
    val verse: Int
)

/**
 * Expands textual verse-range references into individual (book, chapter, verse) tuples.
 *
 * Supported formats:
 *   "3:16"             -> single verse
 *   "18:31-33"         -> verses 31-33 within chapter 18
 *   "1:1-6:11"         -> all verses from ch1:v1 through ch6:v11 (cross-chapter)
 *   "1:1-28:20"        -> entire book range
 */
class VerseExpander {

    private val singleVerse = Regex("""^(\d+):(\d+)$""")
    private val sameChapterRange = Regex("""^(\d+):(\d+)-(\d+)$""")
    private val crossChapterRange = Regex("""^(\d+):(\d+)-(\d+):(\d+)$""")

    fun expand(bookName: String, rangeStr: String): List<SingleVerse> {
        singleVerse.matchEntire(rangeStr)?.let { m ->
            val ch = m.groupValues[1].toInt()
            val v = m.groupValues[2].toInt()
            return listOf(SingleVerse(bookName, ch, v))
        }

        sameChapterRange.matchEntire(rangeStr)?.let { m ->
            val ch = m.groupValues[1].toInt()
            val vStart = m.groupValues[2].toInt()
            val vEnd = m.groupValues[3].toInt()
            return (vStart..vEnd).map { SingleVerse(bookName, ch, it) }
        }

        crossChapterRange.matchEntire(rangeStr)?.let { m ->
            val chStart = m.groupValues[1].toInt()
            val vStart = m.groupValues[2].toInt()
            val chEnd = m.groupValues[3].toInt()
            val vEnd = m.groupValues[4].toInt()
            return expandCrossChapter(bookName, chStart, vStart, chEnd, vEnd)
        }

        throw IllegalArgumentException("Unrecognized verse range format: '$rangeStr' for book $bookName")
    }

    private fun expandCrossChapter(
        bookName: String,
        chStart: Int, vStart: Int,
        chEnd: Int, vEnd: Int
    ): List<SingleVerse> {
        val result = mutableListOf<SingleVerse>()

        for (ch in chStart..chEnd) {
            val maxVerse = CanonicalVerses.versesPerChapter(bookName, ch)
            val from = if (ch == chStart) vStart else 1
            val to = if (ch == chEnd) vEnd else maxVerse
            for (v in from..to) {
                result.add(SingleVerse(bookName, ch, v))
            }
        }

        return result
    }

    fun expandAll(bookName: String, ranges: List<String>): List<SingleVerse> {
        return ranges.flatMap { expand(bookName, it) }
    }
}
