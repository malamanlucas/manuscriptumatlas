package com.ntcoverage.seed

data class BookDefinition(
    val name: String,
    val abbreviation: String,
    val order: Int,
    val chaptersAndVerses: List<Int>
) {
    val totalChapters: Int get() = chaptersAndVerses.size
    val totalVerses: Int get() = chaptersAndVerses.sum()
}

/**
 * Complete canonical structure of the 27 New Testament books.
 * Each entry in chaptersAndVerses is the verse count for that chapter (1-indexed by position).
 * Verse numbering follows the standard traditional scheme (7,956 total verses).
 */
object CanonicalVerses {

    val books: List<BookDefinition> = listOf(
        BookDefinition("Matthew", "Matt", 1, listOf(
            25, 23, 17, 25, 48, 34, 29, 34, 38, 42,
            30, 50, 58, 36, 39, 28, 27, 35, 30, 34,
            46, 46, 39, 51, 46, 75, 66, 20
        )),
        BookDefinition("Mark", "Mark", 2, listOf(
            45, 28, 35, 41, 43, 56, 37, 38, 50, 52,
            33, 44, 37, 72, 47, 20
        )),
        BookDefinition("Luke", "Luke", 3, listOf(
            80, 52, 38, 44, 39, 49, 50, 56, 62, 42,
            54, 59, 35, 35, 32, 31, 37, 43, 48, 47,
            38, 71, 56, 53
        )),
        BookDefinition("John", "John", 4, listOf(
            51, 25, 36, 54, 47, 71, 53, 59, 41, 42,
            57, 50, 38, 31, 27, 33, 26, 40, 42, 31, 25
        )),
        BookDefinition("Acts", "Acts", 5, listOf(
            26, 47, 26, 37, 42, 15, 60, 40, 43, 48,
            30, 25, 52, 28, 41, 40, 34, 28, 41, 38,
            40, 30, 35, 27, 27, 32, 44, 31
        )),
        BookDefinition("Romans", "Rom", 6, listOf(
            32, 29, 31, 25, 21, 23, 25, 39, 33, 21,
            36, 21, 14, 23, 33, 27
        )),
        BookDefinition("1 Corinthians", "1Cor", 7, listOf(
            31, 16, 23, 21, 13, 20, 40, 13, 27, 33,
            34, 31, 13, 40, 58, 24
        )),
        BookDefinition("2 Corinthians", "2Cor", 8, listOf(
            24, 17, 18, 18, 21, 18, 16, 24, 15, 18,
            33, 21, 13
        )),
        BookDefinition("Galatians", "Gal", 9, listOf(24, 21, 29, 31, 26, 18)),
        BookDefinition("Ephesians", "Eph", 10, listOf(23, 22, 21, 32, 33, 24)),
        BookDefinition("Philippians", "Phil", 11, listOf(30, 30, 21, 23)),
        BookDefinition("Colossians", "Col", 12, listOf(29, 23, 25, 18)),
        BookDefinition("1 Thessalonians", "1Thess", 13, listOf(10, 20, 13, 18, 28)),
        BookDefinition("2 Thessalonians", "2Thess", 14, listOf(12, 17, 18)),
        BookDefinition("1 Timothy", "1Tim", 15, listOf(20, 15, 16, 16, 25, 21)),
        BookDefinition("2 Timothy", "2Tim", 16, listOf(18, 26, 17, 22)),
        BookDefinition("Titus", "Titus", 17, listOf(16, 15, 15)),
        BookDefinition("Philemon", "Phlm", 18, listOf(25)),
        BookDefinition("Hebrews", "Heb", 19, listOf(
            14, 18, 19, 16, 14, 20, 28, 13, 28, 39,
            40, 29, 25
        )),
        BookDefinition("James", "Jas", 20, listOf(27, 26, 18, 17, 20)),
        BookDefinition("1 Peter", "1Pet", 21, listOf(25, 25, 22, 19, 14)),
        BookDefinition("2 Peter", "2Pet", 22, listOf(21, 22, 18)),
        BookDefinition("1 John", "1John", 23, listOf(10, 29, 24, 21, 21)),
        BookDefinition("2 John", "2John", 24, listOf(13)),
        BookDefinition("3 John", "3John", 25, listOf(15)),
        BookDefinition("Jude", "Jude", 26, listOf(25)),
        BookDefinition("Revelation", "Rev", 27, listOf(
            20, 29, 22, 11, 14, 17, 17, 13, 21, 11,
            19, 17, 18, 20, 8, 21, 18, 24, 21, 15, 27, 21
        ))
    )

    val totalVerses: Int = books.sumOf { it.totalVerses }

    private val bookByName: Map<String, BookDefinition> =
        books.associateBy { it.name.lowercase() }

    private val bookByAbbreviation: Map<String, BookDefinition> =
        books.associateBy { it.abbreviation.lowercase() }

    fun findBook(nameOrAbbrev: String): BookDefinition? {
        val key = nameOrAbbrev.lowercase().trim()
        return bookByName[key] ?: bookByAbbreviation[key]
    }

    fun versesPerChapter(bookName: String, chapter: Int): Int {
        val book = findBook(bookName) ?: throw IllegalArgumentException("Unknown book: $bookName")
        require(chapter in 1..book.totalChapters) {
            "Chapter $chapter out of range for $bookName (1-${book.totalChapters})"
        }
        return book.chaptersAndVerses[chapter - 1]
    }
}
