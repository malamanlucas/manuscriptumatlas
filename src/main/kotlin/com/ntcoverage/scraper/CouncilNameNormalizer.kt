package com.ntcoverage.scraper

object CouncilNameNormalizer {
    private val ordinalWords = setOf(
        "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth",
        "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x"
    )

    private val removablePrefixes = listOf(
        "council of",
        "synod of",
        "first council of",
        "second council of",
        "third council of",
        "concilium"
    )

    fun normalize(name: String): String {
        var text = name.lowercase()
        removablePrefixes.forEach { prefix ->
            if (text.startsWith(prefix)) {
                text = text.removePrefix(prefix).trim()
            }
        }

        text = text
            .replace(Regex("[()\\[\\],.:;]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        text = text
            .split(" ")
            .filterNot { ordinalWords.contains(it) }
            .joinToString(" ")
            .trim()

        return text
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    fun matchKey(name: String, year: Int): String = "${normalize(name)}-$year"
}
