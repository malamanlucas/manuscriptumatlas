package com.ntcoverage.service

import com.ntcoverage.repository.LexiconRepository
import com.ntcoverage.scraper.BibleHubLexiconScraper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class LexiconEnrichmentService(
    private val lexiconRepository: LexiconRepository,
    private val scraper: BibleHubLexiconScraper
) {
    private val log = LoggerFactory.getLogger(LexiconEnrichmentService::class.java)

    companion object {
        const val MAX_GREEK_STRONGS = 5624
        const val MAX_HEBREW_STRONGS = 8674
        val DELAY_MS = System.getenv("LLM_ENRICHMENT_DELAY_MS")?.toLongOrNull() ?: 100L
    }

    suspend fun enrichGreekLexicon(phaseTracker: IngestionPhaseTracker) {
        enrichLexicon("greek", "G", MAX_GREEK_STRONGS, phaseTracker)
    }

    suspend fun enrichHebrewLexicon(phaseTracker: IngestionPhaseTracker) {
        enrichLexicon("hebrew", "H", MAX_HEBREW_STRONGS, phaseTracker)
    }

    suspend fun reEnrichGreekLexicon(phaseTracker: IngestionPhaseTracker) {
        enrichLexicon("greek", "G", MAX_GREEK_STRONGS, phaseTracker, forceReEnrich = true)
    }

    suspend fun reEnrichHebrewLexicon(phaseTracker: IngestionPhaseTracker) {
        enrichLexicon("hebrew", "H", MAX_HEBREW_STRONGS, phaseTracker, forceReEnrich = true)
    }

    suspend fun fillMissingHebrewEntries(phaseTracker: IngestionPhaseTracker) {
        val phaseName = "bible_fill_missing_hebrew"
        val existing = lexiconRepository.getExistingHebrewStrongsNumbers()
        val missing = (1..MAX_HEBREW_STRONGS)
            .map { "H${it.toString().padStart(4, '0')}" }
            .filter { it !in existing }

        log.info("FILL_MISSING_HEBREW: found ${missing.size} missing entries to scrape")
        phaseTracker.markProgress(phaseName, 0, missing.size)

        val semaphore = Semaphore(System.getenv("LLM_CONCURRENCY")?.toIntOrNull() ?: 15)
        val inserted = AtomicInteger(0)
        val failed = AtomicInteger(0)

        coroutineScope {
            for ((index, sn) in missing.withIndex()) {
                val num = sn.removePrefix("H").toInt()
                launch {
                    semaphore.withPermit {
                        try {
                            val data = scraper.scrapeHebrewEntry(num)
                            if (data != null && (data.lemma != null || data.transliteration != null)) {
                                lexiconRepository.insertHebrewFromBibleHub(
                                    strongsNumber = sn,
                                    lemma = data.lemma ?: data.transliteration ?: sn,
                                    transliteration = data.transliteration,
                                    pronunciation = data.pronunciation,
                                    shortDefinition = data.shortDefinition,
                                    fullDefinition = null,
                                    partOfSpeech = data.partOfSpeech,
                                    phoneticSpelling = data.phoneticSpelling,
                                    kjvTranslation = data.kjvTranslation,
                                    kjvUsageCount = data.kjvUsageCount,
                                    nasbTranslation = data.nasbTranslation,
                                    wordOrigin = data.wordOrigin,
                                    strongsExhaustive = data.strongsExhaustive,
                                    nasExhaustiveOrigin = data.nasExhaustiveOrigin,
                                    nasExhaustiveDefinition = data.nasExhaustiveDefinition,
                                    nasExhaustiveTranslation = data.nasExhaustiveTranslation,
                                    sourceUrl = "https://biblehub.com/hebrew/$num.htm"
                                )
                                inserted.incrementAndGet()
                            } else {
                                failed.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            log.warn("FILL_MISSING_HEBREW: error on $sn: ${e.message}")
                            failed.incrementAndGet()
                        }

                        val current = inserted.get() + failed.get()
                        if (current % 50 == 0) {
                            phaseTracker.markProgress(phaseName, current)
                            log.info("FILL_MISSING_HEBREW: progress [$current/${missing.size}] inserted=${inserted.get()} failed=${failed.get()}")
                        }

                        delay(DELAY_MS)
                    }
                }
            }
        }

        phaseTracker.markProgress(phaseName, missing.size)
        log.info("FILL_MISSING_HEBREW: done. inserted=${inserted.get()} failed=${failed.get()}")
    }

    private suspend fun enrichLexicon(language: String, prefix: String, maxNum: Int, phaseTracker: IngestionPhaseTracker, forceReEnrich: Boolean = false) {
        val phaseName = if (forceReEnrich) "bible_reenrich_${language}_lexicon" else "bible_enrich_${language}_lexicon"
        phaseTracker.markProgress(phaseName, 0, maxNum)

        val semaphore = Semaphore(System.getenv("LLM_CONCURRENCY")?.toIntOrNull() ?: 15)
        val enriched = AtomicInteger(0)
        val skipped = AtomicInteger(0)
        val failed = AtomicInteger(0)

        coroutineScope {
            for (num in 1..maxNum) {
                val strongsNumber = "$prefix${num.toString().padStart(4, '0')}"

                if (!forceReEnrich) {
                    val isEnriched = if (prefix == "G")
                        lexiconRepository.isGreekEntryEnriched(strongsNumber)
                    else
                        lexiconRepository.isHebrewEntryEnriched(strongsNumber)

                    if (isEnriched) {
                        skipped.incrementAndGet()
                        if (num % 500 == 0) {
                            phaseTracker.markProgress(phaseName, num)
                            log.info("LEXICON_ENRICH_${language.uppercase()}: progress [$num/$maxNum] enriched=${enriched.get()} skipped=${skipped.get()} failed=${failed.get()}")
                        }
                        continue
                    }
                }

                launch {
                    semaphore.withPermit {
                        try {
                            val data = if (prefix == "G") scraper.scrapeGreekEntry(num) else scraper.scrapeHebrewEntry(num)
                            if (data != null) {
                                if (prefix == "G") {
                                    lexiconRepository.enrichGreekEntry(
                                        strongsNumber, data.pronunciation, data.phoneticSpelling,
                                        data.kjvTranslation, data.kjvUsageCount, data.nasbTranslation,
                                        data.wordOrigin, data.strongsExhaustive,
                                        data.nasExhaustiveOrigin, data.nasExhaustiveDefinition, data.nasExhaustiveTranslation,
                                        shortDefinition = data.shortDefinition,
                                        sourceUrl = "https://biblehub.com/$language/$num.htm"
                                    )
                                } else {
                                    lexiconRepository.enrichHebrewEntry(
                                        strongsNumber, data.pronunciation, data.phoneticSpelling,
                                        data.kjvTranslation, data.kjvUsageCount, data.nasbTranslation,
                                        data.wordOrigin, data.strongsExhaustive,
                                        data.nasExhaustiveOrigin, data.nasExhaustiveDefinition, data.nasExhaustiveTranslation,
                                        shortDefinition = data.shortDefinition,
                                        sourceUrl = "https://biblehub.com/$language/$num.htm"
                                    )
                                }
                                enriched.incrementAndGet()
                            } else {
                                failed.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            log.warn("LEXICON_ENRICH_${language.uppercase()}: error on $strongsNumber: ${e.message}")
                            failed.incrementAndGet()
                        }

                        val current = enriched.get() + skipped.get() + failed.get()
                        if (current % 50 == 0) {
                            phaseTracker.markProgress(phaseName, current)
                            log.info("LEXICON_ENRICH_${language.uppercase()}: progress [$current/$maxNum] enriched=${enriched.get()} skipped=${skipped.get()} failed=${failed.get()}")
                        }

                        delay(DELAY_MS)
                    }
                }
            }
        }

        phaseTracker.markProgress(phaseName, maxNum)
        log.info("LEXICON_ENRICH_${language.uppercase()}: completed. enriched=${enriched.get()} skipped=${skipped.get()} failed=${failed.get()}")
    }
}
