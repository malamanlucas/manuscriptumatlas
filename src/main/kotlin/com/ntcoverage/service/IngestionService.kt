package com.ntcoverage.service

import com.ntcoverage.config.IngestionConfig
import com.ntcoverage.model.IngestionResult
import com.ntcoverage.model.ManuscriptSeed
import com.ntcoverage.repository.CoverageRepository
import com.ntcoverage.repository.ManuscriptRepository
import com.ntcoverage.repository.VerseRepository
import com.ntcoverage.scraper.NtvmrClient
import com.ntcoverage.scraper.NtvmrListClient
import com.ntcoverage.scraper.NtvmrVerseParser
import com.ntcoverage.seed.CanonicalVerses
import com.ntcoverage.seed.ManuscriptSeedData
import com.ntcoverage.util.NtvmrUrl
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

private data class NtvmrIngestionResult(val versesLinked: Int, val usedNtvmr: Boolean)

class IngestionService(
    private val verseRepository: VerseRepository,
    private val manuscriptRepository: ManuscriptRepository,
    private val coverageRepository: CoverageRepository,
    private val verseExpander: VerseExpander,
    private val ntvmrListClient: NtvmrListClient
) {
    private val log = LoggerFactory.getLogger(IngestionService::class.java)
    private val useNtvmr = System.getenv("USE_NTVMR")?.lowercase() != "false"
    private val ntvmrDelayMs = System.getenv("NTVMR_DELAY_MS")?.toLongOrNull() ?: 500L

    fun seedBooksAndVerses() {
        log.info("Seeding ${CanonicalVerses.books.size} books and ${CanonicalVerses.totalVerses} canonical verses...")

        for (book in CanonicalVerses.books) {
            val bookId = verseRepository.insertBook(
                name = book.name,
                abbreviation = book.abbreviation,
                order = book.order,
                totalChapters = book.totalChapters,
                totalVerses = book.totalVerses
            )

            val verses = mutableListOf<Pair<Int, Int>>()
            for ((chIdx, verseCount) in book.chaptersAndVerses.withIndex()) {
                val chapter = chIdx + 1
                for (v in 1..verseCount) {
                    verses.add(chapter to v)
                }
            }
            verseRepository.insertVerses(bookId, verses)
            log.debug("  Seeded ${book.name}: ${book.totalChapters} chapters, ${book.totalVerses} verses")
        }

        log.info("Canonical seed complete.")
    }

    suspend fun ingestManuscriptsAsync(): IngestionResult {
        log.info("Starting manuscript ingestion...")
        val manuscripts = if (IngestionConfig.loadManuscriptsFromNtvmr) {
            log.info("LOAD_MANUSCRIPTS_FROM_NTVMR=true — fetching catalog from NTVMR API")
            val fetched = ntvmrListClient.fetchManuscripts()
            if (fetched.isEmpty()) {
                log.warn("NTVMR_UNAVAILABLE — API returned 0 manuscripts. Skipping manuscript ingestion.")
                return IngestionResult(manuscriptsIngested = 0, versesLinked = 0)
            }
            fetched
        } else {
            ManuscriptSeedData.load()
        }
        log.info("Loaded ${manuscripts.size} manuscripts from ${if (IngestionConfig.loadManuscriptsFromNtvmr) "NTVMR catalog" else "seed data"}")

        val result = if (useNtvmr) {
            log.info("=== NTVMR integration ENABLED — fetching precise verse data from INTF Münster ===")
            ingestWithNtvmr(manuscripts)
        } else {
            log.info("=== NTVMR integration DISABLED — using seed JSON ranges ===")
            ingestFromSeedOnly(manuscripts)
        }

        log.info("Manuscript ingestion complete: ${result.manuscriptsIngested} manuscripts, ${result.versesLinked} verses linked")
        return result
    }

    suspend fun materializeCoverageAsync() {
        log.info("Materializing coverage cache for centuries I-X...")
        coverageRepository.clearCoverageCache()
        val bookIds = coverageRepository.getAllBookIds()

        var latestCoverage: List<com.ntcoverage.model.BookCoverage>? = null
        for (century in 1..10) {
            val coverage = coverageRepository.calculateCoverage(century)
            val pairs = coverage.mapNotNull { cov ->
                val bookId = bookIds[cov.bookName]
                if (bookId != null) bookId to cov else null
            }
            coverageRepository.materializeCoverage(century, pairs)
            val totalCovered = coverage.sumOf { it.coveredVerses }
            log.info("  Century $century: $totalCovered verses covered across ${coverage.size} books")
            if (century == 10) latestCoverage = coverage
        }

        latestCoverage?.let { coverage ->
            val incomplete = coverage.filter { it.coveragePercent < 100.0 }
            if (incomplete.isEmpty()) {
                log.info("COVERAGE_DIAGNOSTIC: All ${coverage.size} books fully attested at century X")
            } else {
                log.warn("COVERAGE_DIAGNOSTIC: ${incomplete.size} book(s) not fully attested at century X:")
                for (book in incomplete) {
                    val missing = book.totalVerses - book.coveredVerses
                    log.warn("  ${book.bookName}: ${book.coveredVerses}/${book.totalVerses} (${book.coveragePercent}%) — $missing verse(s) missing")
                }
            }
        }
    }

    private suspend fun ingestWithNtvmr(manuscripts: List<ManuscriptSeed>): IngestionResult {
        val parser = NtvmrVerseParser()
        var ntvmrSuccessCount = 0
        var seedFallbackCount = 0
        var skippedCount = 0
        var ntvmrAvailable = true
        var totalVersesLinked = 0

        val seedLookup = ManuscriptSeedData.load().associateBy { it.gaId }
        log.info("Loaded ${seedLookup.size} seed manuscripts as fallback")

        val client = NtvmrClient()
        try {
            for (ms in manuscripts) {
                if (ms.centuryMin > 10) {
                    log.debug("Skipping ${ms.gaId} (century ${ms.centuryMin} > X)")
                    continue
                }

                val effectiveMs = if (ms.content.isEmpty()) seedLookup[ms.gaId] ?: ms else ms

                val manuscriptId = manuscriptRepository.insertIfNotExists(
                    gaId = ms.gaId,
                    name = ms.name,
                    centuryMin = ms.centuryMin,
                    centuryMax = ms.centuryMax,
                    manuscriptType = ms.type
                )

                if (!ntvmrAvailable) {
                    val linked = linkFromSeed(manuscriptId, effectiveMs)
                    if (linked > 0) {
                        totalVersesLinked += linked
                        seedFallbackCount++
                    } else {
                        skippedCount++
                    }
                    continue
                }

                val result = tryNtvmrIngestion(client, parser, manuscriptId, effectiveMs)
                if (result != null) {
                    val source = if (result.usedNtvmr) "NTVMR" else "seed"
                    log.info("${ms.gaId} ($source, century ${ms.centuryMin}): linked ${result.versesLinked} verses")
                    totalVersesLinked += result.versesLinked
                    if (result.usedNtvmr) ntvmrSuccessCount++ else seedFallbackCount++
                } else if (effectiveMs.content.isNotEmpty()) {
                    val seedLinked = linkFromSeed(manuscriptId, effectiveMs)
                    log.info("${ms.gaId} (seed fallback, century ${ms.centuryMin}): linked $seedLinked verses")
                    totalVersesLinked += seedLinked
                    seedFallbackCount++
                    if (ntvmrSuccessCount == 0 && seedFallbackCount >= 3) {
                        log.warn("NTVMR appears unavailable after $seedFallbackCount failures — switching to seed-only mode")
                        ntvmrAvailable = false
                    }
                } else {
                    skippedCount++
                    log.debug("${ms.gaId}: no transcript in NTVMR and no seed content")
                }

                delay(ntvmrDelayMs)
            }
        } finally {
            client.close()
        }

        log.info("=== Ingestion summary: $ntvmrSuccessCount from NTVMR, $seedFallbackCount from seed, $skippedCount skipped ===")
        return IngestionResult(
            manuscriptsIngested = ntvmrSuccessCount + seedFallbackCount,
            versesLinked = totalVersesLinked
        )
    }

    private suspend fun tryNtvmrIngestion(
        client: NtvmrClient,
        parser: NtvmrVerseParser,
        manuscriptId: Int,
        ms: ManuscriptSeed
    ): NtvmrIngestionResult? {
        val docId = NtvmrUrl.gaIdToDocId(ms.gaId)

        if (ms.content.isEmpty()) {
            val linked = ingestFromFullTranscript(client, parser, manuscriptId, ms.gaId, docId)
            return if (linked != null) NtvmrIngestionResult(linked, usedNtvmr = true) else null
        }

        var totalLinked = 0
        var anySuccess = false

        for (bookContent in ms.content) {
            val bookDef = CanonicalVerses.findBook(bookContent.book) ?: continue
            val bookId = verseRepository.findBookIdByName(bookDef.name) ?: continue

            val teiXml = client.fetchBookTranscript(docId, bookDef.name)

            if (teiXml != null && teiXml.contains("<ab")) {
                val presentVerses = parser.extractPresentVerses(teiXml)
                val bookVerses = presentVerses[bookDef.name]

                if (bookVerses != null && bookVerses.isNotEmpty()) {
                    val linked = linkVersesByChapterVerse(manuscriptId, bookId, bookVerses)
                    totalLinked += linked
                    anySuccess = true
                    log.debug("  ${ms.gaId}/${bookDef.name}: NTVMR -> $linked verses (${bookVerses.size} reported present)")
                    delay(ntvmrDelayMs)
                    continue
                }
            }

            val seedLinked = linkBookFromSeed(manuscriptId, bookId, bookDef.name, bookContent.ranges)
            totalLinked += seedLinked
            log.debug("  ${ms.gaId}/${bookDef.name}: seed fallback -> $seedLinked verses")
            delay(ntvmrDelayMs)
        }

        return if (totalLinked > 0) NtvmrIngestionResult(totalLinked, usedNtvmr = anySuccess) else null
    }

    private suspend fun ingestFromFullTranscript(
        client: NtvmrClient,
        parser: NtvmrVerseParser,
        manuscriptId: Int,
        gaId: String,
        docId: String
    ): Int? {
        val teiXml = client.fetchTranscript(docId, null) ?: return null
        if (!teiXml.contains("<ab")) return null
        val presentVerses = parser.extractPresentVerses(teiXml)
        var totalLinked = 0
        for ((bookName, verses) in presentVerses) {
            if (verses.isEmpty()) continue
            val bookId = verseRepository.findBookIdByName(bookName) ?: continue
            val linked = linkVersesByChapterVerse(manuscriptId, bookId, verses)
            totalLinked += linked
            log.debug("  $gaId/$bookName: full transcript -> $linked verses")
        }
        return if (totalLinked > 0) totalLinked else null
    }

    private fun linkVersesByChapterVerse(
        manuscriptId: Int,
        bookId: Int,
        verses: List<Pair<Int, Int>>
    ): Int {
        val verseLookup = verseRepository.loadAllVersesForBook(bookId)
            .associateBy { it.chapter to it.verse }

        val verseIds = verses.mapNotNull { (chapter, verse) ->
            verseLookup[chapter to verse]?.verseId
        }

        if (verseIds.isNotEmpty()) {
            verseRepository.insertManuscriptVerses(manuscriptId, verseIds)
        }
        return verseIds.size
    }

    private fun linkBookFromSeed(
        manuscriptId: Int,
        bookId: Int,
        bookName: String,
        ranges: List<String>
    ): Int {
        val verseLookup = verseRepository.loadAllVersesForBook(bookId)
            .associateBy { Triple(it.bookId, it.chapter, it.verse) }

        val expandedVerses = verseExpander.expandAll(bookName, ranges)
        val verseIds = expandedVerses.mapNotNull { sv ->
            val key = Triple(bookId, sv.chapter, sv.verse)
            verseLookup[key]?.verseId
        }

        if (verseIds.isNotEmpty()) {
            verseRepository.insertManuscriptVerses(manuscriptId, verseIds)
        }
        return verseIds.size
    }

    private fun linkFromSeed(manuscriptId: Int, ms: ManuscriptSeed): Int {
        var totalLinked = 0
        for (bookContent in ms.content) {
            val bookDef = CanonicalVerses.findBook(bookContent.book) ?: continue
            val bookId = verseRepository.findBookIdByName(bookDef.name) ?: continue
            totalLinked += linkBookFromSeed(manuscriptId, bookId, bookDef.name, bookContent.ranges)
        }
        return totalLinked
    }

    private fun ingestFromSeedOnly(manuscripts: List<ManuscriptSeed>): IngestionResult {
        var manuscriptsIngested = 0
        var totalVersesLinked = 0

        for (ms in manuscripts) {
            if (ms.centuryMin > 10) {
                log.debug("Skipping ${ms.gaId} (century ${ms.centuryMin} > X)")
                continue
            }

            val manuscriptId = manuscriptRepository.insertIfNotExists(
                gaId = ms.gaId,
                name = ms.name,
                centuryMin = ms.centuryMin,
                centuryMax = ms.centuryMax,
                manuscriptType = ms.type
            )

            val totalLinked = linkFromSeed(manuscriptId, ms)
            totalVersesLinked += totalLinked
            manuscriptsIngested++
            log.info("${ms.gaId} (seed, century ${ms.centuryMin}): linked $totalLinked verses")
        }

        return IngestionResult(
            manuscriptsIngested = manuscriptsIngested,
            versesLinked = totalVersesLinked
        )
    }
}
