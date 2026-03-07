package com.ntcoverage.service

import com.ntcoverage.config.SourceFileCache
import com.ntcoverage.model.CouncilFathers
import com.ntcoverage.model.Councils
import com.ntcoverage.model.Heresies
import com.ntcoverage.repository.*
import com.ntcoverage.scraper.CouncilSourceExtractor
import com.ntcoverage.seed.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class CouncilIngestionService(
    private val councilRepository: CouncilRepository,
    private val heresyRepository: HeresyRepository,
    private val canonRepository: CouncilCanonRepository,
    private val hereticParticipantRepository: CouncilHereticParticipantRepository,
    private val sourceRepository: SourceRepository,
    private val claimRepository: CouncilSourceClaimRepository,
    private val churchFatherRepository: ChurchFatherRepository,
    private val extractors: List<CouncilSourceExtractor>,
    private val consensusEngine: SourceConsensusEngine,
    private val summarizationService: BiographySummarizationService,
    private val phaseTracker: CouncilPhaseTracker,
    private val fileCache: SourceFileCache
) {
    private val log = LoggerFactory.getLogger(CouncilIngestionService::class.java)

    suspend fun phase1Seed() = runPhaseTracked("council_seed", runBy = "manual") {
        log.info("COUNCIL_SEED: inserting {} sources", SourcesSeedData.entries.size)
        val sourceTotal = SourcesSeedData.entries.size
        var processed = 0
        SourcesSeedData.entries.forEach { source ->
            sourceRepository.insertOrUpdate(source)
            processed++
            phaseTracker.markProgress("council_seed", processed, sourceTotal)
        }
        log.info("COUNCIL_SEED: sources done ({})", processed)

        log.info("COUNCIL_SEED: inserting {} heresies", HeresiesSeedData.entries.size)
        HeresiesSeedData.entries.forEach { heresyRepository.insertOrUpdate(it) }
        HeresyTranslationsSeedData.entries.forEach { heresyRepository.insertOrUpdateTranslation(it) }
        log.info("COUNCIL_SEED: heresies + translations done")

        val seedSourceId = sourceRepository.findIdByName("seed")
        log.info("COUNCIL_SEED: inserting {} councils (seedSourceId={})", CouncilsSeedData.entries.size, seedSourceId)
        var totalFathersLinked = 0
        var totalFathersNotFound = 0
        var totalHeresiesLinked = 0
        var totalHeresiesNotFound = 0

        CouncilsSeedData.entries.forEachIndexed { idx, council ->
            try {
                val councilId = councilRepository.insertOrUpdate(council)

                council.heresyNames.forEach { heresyName ->
                    val normalized = normalizeHeresyName(heresyName)
                    val heresyId = heresyRepository.findByNormalizedName(normalized)
                    if (heresyId != null) {
                        heresyRepository.linkCouncilHeresy(councilId, heresyId, action = "condemned")
                        totalHeresiesLinked++
                    } else {
                        totalHeresiesNotFound++
                        log.warn("COUNCIL_SEED: heresy not found for council '{}': normalizedName='{}' (raw='{}')", council.displayName, normalized, heresyName)
                    }
                }

                council.fatherNames.forEach { fatherName ->
                    val father = churchFatherRepository.findByNormalizedName(fatherName)
                    if (father != null) {
                        linkCouncilFather(councilId, father.id)
                        totalFathersLinked++
                        log.debug("COUNCIL_SEED: linked father '{}' (id={}) to council '{}'", fatherName, father.id, council.displayName)
                    } else {
                        totalFathersNotFound++
                        log.warn("COUNCIL_SEED: father NOT FOUND for council '{}': normalizedName='{}'", council.displayName, fatherName)
                    }
                }

                if (seedSourceId != null) {
                    claimRepository.upsertClaim(
                        councilId = councilId,
                        sourceId = seedSourceId,
                        claimedYear = council.year,
                        claimedYearEnd = council.yearEnd,
                        claimedLocation = council.location,
                        claimedParticipants = council.numberOfParticipants,
                        rawText = council.shortDescription,
                        sourcePage = "seed"
                    )
                }

                if ((idx + 1) % 20 == 0 || idx == CouncilsSeedData.entries.size - 1) {
                    log.info("COUNCIL_SEED: progress {}/{} (last: {})", idx + 1, CouncilsSeedData.entries.size, council.displayName)
                }
                phaseTracker.markProgress("council_seed", idx + 1, CouncilsSeedData.entries.size)
            } catch (e: Exception) {
                log.error("COUNCIL_SEED: failed on council '{}' (year={}): {}", council.displayName, council.year, e.message)
                throw e
            }
        }

        log.info("COUNCIL_SEED: father links — linked={}, notFound={}", totalFathersLinked, totalFathersNotFound)
        log.info("COUNCIL_SEED: heresy links — linked={}, notFound={}", totalHeresiesLinked, totalHeresiesNotFound)

        log.info("COUNCIL_SEED: inserting {} translations", CouncilTranslationsSeedData.entries.size)
        var translationsApplied = 0
        CouncilTranslationsSeedData.entries.forEach { translation ->
            val councilId = councilRepository.findIdBySlug(translation.councilSlug)
            if (councilId == null) {
                log.warn("COUNCIL_SEED: translation slug not found: '{}' (locale={})", translation.councilSlug, translation.locale)
                return@forEach
            }
            councilRepository.insertOrUpdateTranslation(
                councilId = councilId,
                locale = translation.locale,
                displayName = translation.displayName,
                shortDescription = translation.shortDescription,
                location = translation.location,
                mainTopics = translation.mainTopics,
                summary = translation.summary,
                translationSource = "seed"
            )
            translationsApplied++
        }
        log.info("COUNCIL_SEED: translations applied={} of {} total entries", translationsApplied, CouncilTranslationsSeedData.entries.size)

        log.info("COUNCIL_SEED: inserting {} canons", CouncilCanonsSeedData.entries.size)
        var canonsInserted = 0
        CouncilCanonsSeedData.entries.forEach { entry ->
            val councilId = councilRepository.findIdBySlug(entry.councilSlug)
            if (councilId == null) {
                log.debug("COUNCIL_SEED: canon slug not found: '{}' (canon {})", entry.councilSlug, entry.canonNumber)
                return@forEach
            }
            val inserted = canonRepository.insertIfMissing(
                councilId = councilId,
                canonNumber = entry.canonNumber,
                title = entry.title,
                canonText = entry.canonText,
                topic = entry.topic
            )
            if (inserted) canonsInserted++
        }
        log.info("COUNCIL_SEED: canons inserted={} of {} entries", canonsInserted, CouncilCanonsSeedData.entries.size)

        log.info("COUNCIL_SEED: inserting {} heretic participants", CouncilHereticParticipantsSeedData.entries.size)
        var hereticsInserted = 0
        CouncilHereticParticipantsSeedData.entries.forEach { entry ->
            val councilId = councilRepository.findIdBySlug(entry.councilSlug)
            if (councilId == null) {
                log.debug("COUNCIL_SEED: heretic slug not found: '{}' (participant: {})", entry.councilSlug, entry.displayName)
                return@forEach
            }
            val inserted = hereticParticipantRepository.insertIfNotExists(
                councilId = councilId,
                displayName = entry.displayName,
                normalizedName = entry.normalizedName,
                role = entry.role,
                description = entry.description
            )
            if (inserted) hereticsInserted++
        }
        log.info("COUNCIL_SEED: heretic participants inserted={} of {} entries", hereticsInserted, CouncilHereticParticipantsSeedData.entries.size)
    }

    suspend fun phase2aSchaff() = processExtractorPhase("council_extract_schaff", "schaff")
    suspend fun phase2bHefele() = processExtractorPhase("council_extract_hefele", "hefele")
    suspend fun phase2cCatholicEnc() = processExtractorPhase("council_extract_catholic_enc", "catholic_encyclopedia")
    suspend fun phase2dFordham() = processExtractorPhase("council_extract_fordham", "fordham")
    suspend fun phase3Wikidata() = processExtractorPhase("council_extract_wikidata", "wikidata")
    suspend fun phase4Wikipedia() = processExtractorPhase("council_extract_wikipedia", "wikipedia", saveOriginalText = true)

    suspend fun phase5Consensus() = runPhaseTracked("council_consensus", runBy = "manual") {
        val councilRows = transaction {
            Councils.selectAll().map { it[Councils.id].value to it[Councils.displayName] }
        }
        log.info("COUNCIL_CONSENSUS: calculating consensus for {} councils", councilRows.size)
        councilRows.forEachIndexed { idx, (councilId, councilName) ->
            val result = consensusEngine.calculateConsensus(councilId, councilName)
            councilRepository.updateConsensus(
                councilId = councilId,
                year = result.consensusYear,
                yearEnd = result.consensusYearEnd,
                location = result.consensusLocation,
                participants = result.consensusParticipants,
                confidence = result.confidenceScore,
                dataConfidence = result.dataConfidence,
                sourceCount = result.sourceCount,
                conflictResolution = result.conflictResolution
            )
            if ((idx + 1) % 25 == 0 || idx == councilRows.size - 1) {
                log.info("COUNCIL_CONSENSUS: progress {}/{} (last: {} confidence={})", idx + 1, councilRows.size, councilName, String.format("%.2f", result.confidenceScore))
            }
            phaseTracker.markProgress("council_consensus", idx + 1, councilRows.size)
        }

        log.info("COUNCIL_CONSENSUS: updating source reliability scores")
        sourceRepository.findAll().forEach { source ->
            consensusEngine.updateSourceReliability(source.id)
        }
        log.info("COUNCIL_CONSENSUS: done")
    }

    suspend fun phase6Summaries(limit: Int = 20) = runPhaseTracked("council_summaries", runBy = "manual") {
        val rows = transaction {
            Councils.selectAll()
                .where { Councils.summary.isNull() and Councils.originalText.isNotNull() }
                .limit(limit)
                .map { row ->
                    SummaryTarget(
                        id = row[Councils.id].value,
                        displayName = row[Councils.displayName],
                        originalText = row[Councils.originalText]
                    )
                }
        }
        log.info("COUNCIL_SUMMARIES: {} councils need summarization (limit={})", rows.size, limit)

        rows.forEachIndexed { idx, target ->
            val original = target.originalText ?: return@forEachIndexed
            log.info("COUNCIL_SUMMARIES: summarizing '{}'", target.displayName)
            val summary = summarizationService.summarizeIfNeeded(original)
            if (!summary.isNullOrBlank()) {
                councilRepository.updateSummary(target.id, summary, reviewed = false)
                listOf("pt", "es").forEach { locale ->
                    val translated = summarizationService.translateBiography(summary, locale, target.displayName)
                    if (!translated.isNullOrBlank()) {
                        councilRepository.insertOrUpdateTranslation(
                            councilId = target.id,
                            locale = locale,
                            displayName = councilRepository.findById(target.id, "en")?.displayName ?: target.displayName,
                            summary = translated,
                            translationSource = "machine"
                        )
                    }
                }
                log.info("COUNCIL_SUMMARIES: done '{}'", target.displayName)
            } else {
                log.warn("COUNCIL_SUMMARIES: empty summary for '{}'", target.displayName)
            }
            phaseTracker.markProgress("council_summaries", idx + 1, rows.size)
        }
        log.info("COUNCIL_SUMMARIES: finished {} councils", rows.size)
    }

    suspend fun phase7TranslateAll(limit: Int = 100) = runPhaseTracked("council_translate_all", runBy = "manual") {
        val councilRows = transaction {
            Councils.selectAll()
                .orderBy(Councils.year to SortOrder.ASC, Councils.id to SortOrder.ASC)
                .limit(limit)
                .map { row ->
                    CouncilTranslateTarget(
                        id = row[Councils.id].value,
                        displayName = row[Councils.displayName],
                        shortDescription = row[Councils.shortDescription],
                        location = row[Councils.location],
                        mainTopics = row[Councils.mainTopics],
                        summary = row[Councils.summary]
                    )
                }
        }
        log.info("COUNCIL_TRANSLATE_ALL: translating {} councils to pt/es (limit={})", councilRows.size, limit)

        var translated = 0
        councilRows.forEachIndexed { idx, target ->
            listOf("pt", "es").forEach { locale ->
                val meta = councilRepository.findTranslationMeta(target.id, locale)
                if (meta?.translationSource in listOf("reviewed", "seed")) {
                    log.debug("COUNCIL_TRANSLATE_ALL: skipping councilId={} locale={} (source={})", target.id, locale, meta?.translationSource)
                    return@forEach
                }

                val displayName = target.displayName
                if (displayName.isBlank()) return@forEach

                log.info("COUNCIL_TRANSLATE_ALL: translating '{}' to {}", displayName, locale)
                val result = summarizationService.translateCouncilFields(
                    displayName = displayName,
                    shortDescription = target.shortDescription,
                    location = target.location,
                    mainTopics = target.mainTopics,
                    summary = target.summary,
                    targetLocale = locale,
                    councilName = displayName
                )
                if (result != null && result.isNotEmpty()) {
                    councilRepository.insertOrUpdateTranslation(
                        councilId = target.id,
                        locale = locale,
                        displayName = result["displayName"] ?: displayName,
                        shortDescription = result["shortDescription"],
                        location = result["location"],
                        mainTopics = result["mainTopics"],
                        summary = result["summary"],
                        translationSource = "machine"
                    )
                    translated++
                }
            }
            phaseTracker.markProgress("council_translate_all", idx + 1, councilRows.size)
        }
        log.info("COUNCIL_TRANSLATE_ALL: finished {} councils, {} translations applied", councilRows.size, translated)
    }

    suspend fun phase8TranslateHeresies(limit: Int = 50) = runPhaseTracked("heresy_translate_all", runBy = "manual") {
        val heresyRows = transaction {
            Heresies.selectAll()
                .orderBy(Heresies.centuryOrigin to SortOrder.ASC_NULLS_LAST, Heresies.name to SortOrder.ASC)
                .limit(limit)
                .map { row ->
                    HeresyTranslateTarget(
                        id = row[Heresies.id].value,
                        name = row[Heresies.name],
                        description = row[Heresies.description]
                    )
                }
        }
        log.info("HERESY_TRANSLATE_ALL: translating {} heresies to pt/es (limit={})", heresyRows.size, limit)

        var translated = 0
        heresyRows.forEachIndexed { idx, target ->
            listOf("pt", "es").forEach { locale ->
                val meta = heresyRepository.findTranslationMeta(target.id, locale)
                if (meta?.translationSource in listOf("reviewed", "seed")) {
                    log.debug("HERESY_TRANSLATE_ALL: skipping heresyId={} locale={} (source={})", target.id, locale, meta?.translationSource)
                    return@forEach
                }

                val name = target.name
                if (name.isBlank()) return@forEach

                log.info("HERESY_TRANSLATE_ALL: translating '{}' to {}", name, locale)
                val result = summarizationService.translateHeresyFields(
                    name = name,
                    description = target.description,
                    targetLocale = locale,
                    heresyName = name
                )
                if (result != null && result.isNotEmpty()) {
                    heresyRepository.insertOrUpdateTranslation(
                        heresyId = target.id,
                        locale = locale,
                        name = result["name"] ?: name,
                        description = result["description"],
                        translationSource = "machine"
                    )
                    translated++
                }
            }
            phaseTracker.markProgress("heresy_translate_all", idx + 1, heresyRows.size)
        }
        log.info("HERESY_TRANSLATE_ALL: finished {} heresies, {} translations applied", heresyRows.size, translated)
    }

    private val wikiJson = Json { ignoreUnknownKeys = true }

    suspend fun phase9OverviewEnrichment(limit: Int = 50) = runPhaseTracked("council_overview_enrichment", runBy = "manual") {
        val rows = transaction {
            Councils.selectAll()
                .where { Councils.summary.isNull() and Councils.originalText.isNull() }
                .orderBy(Councils.year to SortOrder.ASC, Councils.id to SortOrder.ASC)
                .limit(limit)
                .map { row ->
                    EnrichmentTarget(
                        id = row[Councils.id].value,
                        displayName = row[Councils.displayName],
                        year = row[Councils.year],
                        location = row[Councils.location],
                        councilType = row[Councils.councilType],
                        mainTopics = row[Councils.mainTopics]
                    )
                }
        }
        log.info("COUNCIL_OVERVIEW_ENRICHMENT: {} councils need overview (limit={})", rows.size, limit)

        val wikipediaSourceId = sourceRepository.findIdByName("wikipedia")
        val aiEnrichmentSourceId = sourceRepository.findIdByName("ai_enrichment")

        rows.forEachIndexed { idx, target ->
            var enriched = false

            // Try Wikipedia search first
            val searchQuery = "${target.displayName} ${target.year}"
            val wikiTitle = searchWikipediaTitle(searchQuery)
            if (wikiTitle != null) {
                val wikiUrl = "https://en.wikipedia.org/wiki/${wikiTitle.replace(" ", "_")}"
                val text = fetchWikipediaPage(wikiUrl)
                if (!text.isNullOrBlank() && text.length >= 100) {
                    val originalText = text.take(5_000)
                    councilRepository.updateOriginalText(target.id, originalText)
                    if (wikipediaSourceId != null) {
                        claimRepository.upsertClaim(
                            councilId = target.id,
                            sourceId = wikipediaSourceId,
                            claimedYear = target.year,
                            claimedLocation = target.location,
                            rawText = text.take(2_000),
                            sourcePage = wikiUrl
                        )
                    }
                    enriched = true
                    log.info("COUNCIL_OVERVIEW_ENRICHMENT: [{}] Wikipedia '{}' -> {}", target.displayName, wikiTitle, wikiUrl)
                    // Generate summary and translate to pt/es for i18n
                    val summaryEn = summarizationService.summarizeIfNeeded(originalText)
                    if (!summaryEn.isNullOrBlank()) {
                        councilRepository.updateSummary(target.id, summaryEn, reviewed = false)
                        translateAndSaveCouncilSummary(target.id, target.displayName, summaryEn)
                    }
                }
            }

            // Fallback to AI if Wikipedia did not yield content
            if (!enriched) {
                val summary = summarizationService.generateCouncilOverviewFromMetadata(
                    displayName = target.displayName,
                    year = target.year,
                    location = target.location,
                    councilType = target.councilType,
                    mainTopics = target.mainTopics
                )
                if (!summary.isNullOrBlank()) {
                    councilRepository.updateSummary(target.id, summary, reviewed = false)
                    if (aiEnrichmentSourceId != null) {
                        claimRepository.upsertClaim(
                            councilId = target.id,
                            sourceId = aiEnrichmentSourceId,
                            claimedYear = target.year,
                            claimedLocation = target.location,
                            rawText = summary,
                            sourcePage = "ai_enrichment"
                        )
                    }
                    enriched = true
                    log.info("COUNCIL_OVERVIEW_ENRICHMENT: [{}] AI-generated summary", target.displayName)
                    // Translate to pt/es for i18n
                    translateAndSaveCouncilSummary(target.id, target.displayName, summary)
                }
            }

            if (!enriched) {
                log.debug("COUNCIL_OVERVIEW_ENRICHMENT: [{}] no enrichment available", target.displayName)
            }
            phaseTracker.markProgress("council_overview_enrichment", idx + 1, rows.size)
        }
        log.info("COUNCIL_OVERVIEW_ENRICHMENT: finished {} councils", rows.size)
    }

    private data class EnrichmentTarget(
        val id: Int,
        val displayName: String,
        val year: Int,
        val location: String?,
        val councilType: String?,
        val mainTopics: String?
    )

    private suspend fun searchWikipediaTitle(searchQuery: String): String? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8)
            val url = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&srlimit=1"
            val json = fileCache.getOrFetch("wikipedia_search/${searchQuery.hashCode()}.json") {
                java.net.URL(url).openConnection().apply {
                    connectTimeout = 5_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "ManuscriptumAtlasBot/1.0 (historical research)")
                }.getInputStream().bufferedReader().readText()
            }
            val root = wikiJson.parseToJsonElement(json).jsonObject
            val search = root["query"]?.jsonObject?.get("search")?.jsonArray ?: return@withContext null
            val first = search.firstOrNull()?.jsonObject ?: return@withContext null
            first["title"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            log.debug("COUNCIL_OVERVIEW_ENRICHMENT: Wikipedia search failed for '{}': {}", searchQuery, e.message)
            null
        }
    }

    private suspend fun fetchWikipediaPage(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val pageSlug = url.substringAfterLast("/")
            val cacheKey = "wikipedia/$pageSlug.html"
            val html = fileCache.getOrFetch(cacheKey) {
                Jsoup.connect(url)
                    .userAgent("ManuscriptumAtlasBot/1.0 (historical research)")
                    .timeout(20_000)
                    .ignoreContentType(true)
                    .execute()
                    .body()
            }
            val doc = Jsoup.parse(html)
            doc.select("#mw-content-text p")
                .filterNot { it.select("sup.reference, span.mw-editsection").isNotEmpty() }
                .joinToString("\n") { it.text() }
                .ifBlank { doc.text() }
                .take(5_000)
                .ifBlank { null }
        } catch (e: Exception) {
            log.debug("COUNCIL_OVERVIEW_ENRICHMENT: Wikipedia fetch failed for '{}': {}", url, e.message)
            null
        }
    }

    suspend fun runPhases(phases: List<String>) {
        val runStartedAt = System.currentTimeMillis()
        log.info("COUNCIL_INGESTION: starting {} phases: {}", phases.size, phases)
        for ((idx, phase) in phases.withIndex()) {
            val phaseStartedAt = System.currentTimeMillis()
            log.info("COUNCIL_INGESTION: >>> phase {}/{} '{}' START", idx + 1, phases.size, phase)
            when (phase) {
                "council_seed" -> phase1Seed()
                "council_extract_schaff" -> phase2aSchaff()
                "council_extract_hefele" -> phase2bHefele()
                "council_extract_catholic_enc" -> phase2cCatholicEnc()
                "council_extract_fordham" -> phase2dFordham()
                "council_extract_wikidata" -> phase3Wikidata()
                "council_extract_wikipedia" -> phase4Wikipedia()
                "council_consensus" -> phase5Consensus()
                "council_summaries" -> phase6Summaries()
                "council_translate_all" -> phase7TranslateAll()
                "heresy_translate_all" -> phase8TranslateHeresies()
                "council_overview_enrichment" -> phase9OverviewEnrichment()
            }
            val phaseElapsed = System.currentTimeMillis() - phaseStartedAt
            val pct = ((idx + 1) * 100) / phases.size
            log.info(
                "COUNCIL_INGESTION: <<< phase {}/{} '{}' END elapsed={} overallProgress={}%",
                idx + 1, phases.size, phase, formatDuration(phaseElapsed), pct
            )
        }
        val totalElapsed = System.currentTimeMillis() - runStartedAt
        log.info("COUNCIL_INGESTION: all {} phases completed in {}", phases.size, formatDuration(totalElapsed))
    }

    suspend fun fullIngestion() = runPhases(ALL_PHASES)

    fun getCacheStats() = fileCache.getStats()

    private suspend fun processExtractorPhase(
        phaseName: String,
        sourceName: String,
        saveOriginalText: Boolean = false
    ) = runPhaseTracked(phaseName, runBy = "manual") {
        val extractor = extractors.firstOrNull { it.sourceName == sourceName }
        if (extractor == null) {
            log.error("COUNCIL_EXTRACT: no extractor found for source '{}'", sourceName)
            return@runPhaseTracked
        }
        val sourceId = sourceRepository.findIdByName(sourceName)
        if (sourceId == null) {
            log.error("COUNCIL_EXTRACT: source '{}' not found in DB", sourceName)
            return@runPhaseTracked
        }

        log.info("COUNCIL_EXTRACT: [{}] starting extraction", sourceName)
        val extractionStartedAt = System.currentTimeMillis()
        val claims = extractor.extract()
        val extractionElapsed = System.currentTimeMillis() - extractionStartedAt
        log.info(
            "COUNCIL_EXTRACT: [{}] got {} claims in {}, matching to councils",
            sourceName, claims.size, formatDuration(extractionElapsed)
        )

        var matched = 0
        var unmatched = 0
        val matchingStartedAt = System.currentTimeMillis()
        claims.forEachIndexed { idx, claim ->
            val councilId = resolveCouncilIdForClaim(claim)
            if (councilId != null) {
                claimRepository.upsertClaim(
                    councilId = councilId,
                    sourceId = sourceId,
                    claimedYear = claim.claimedYear,
                    claimedYearEnd = claim.claimedYearEnd,
                    claimedLocation = claim.claimedLocation,
                    claimedParticipants = claim.claimedParticipants,
                    rawText = claim.rawText,
                    sourcePage = claim.sourcePage
                )
                if (saveOriginalText && !claim.rawText.isNullOrBlank()) {
                    councilRepository.updateOriginalText(councilId, claim.rawText)
                }
                matched++
            } else {
                log.debug("COUNCIL_EXTRACT: [{}] unmatched claim: '{}' (key={})", sourceName, claim.councilNameRaw, claim.normalizedKey)
                unmatched++
            }
            phaseTracker.markProgress(phaseName, idx + 1, claims.size)
            if ((idx + 1) % 10 == 0 || idx == claims.lastIndex) {
                val pct = if (claims.isNotEmpty()) ((idx + 1) * 100) / claims.size else 100
                log.info(
                    "COUNCIL_EXTRACT: [{}] match progress {}/{} ({}%) matched={} unmatched={}",
                    sourceName, idx + 1, claims.size, pct, matched, unmatched
                )
            }
        }
        val matchingElapsed = System.currentTimeMillis() - matchingStartedAt
        log.info(
            "COUNCIL_EXTRACT: [{}] done — matched={}, unmatched={}, total={}, matchingElapsed={}",
            sourceName, matched, unmatched, claims.size, formatDuration(matchingElapsed)
        )
    }

    private suspend fun runPhaseTracked(
        phaseName: String,
        runBy: String = "manual",
        block: suspend () -> Unit
    ) {
        val phaseStartedAt = System.currentTimeMillis()
        log.info("PHASE_TRACKER: '{}' → RUNNING (by={})", phaseName, runBy)
        phaseTracker.markRunning(phaseName, runBy = runBy)
        try {
            block()
            val status = phaseTracker.getPhaseStatus(phaseName)
            phaseTracker.markSuccess(phaseName, status?.itemsProcessed ?: 0)
            val elapsed = System.currentTimeMillis() - phaseStartedAt
            log.info(
                "PHASE_TRACKER: '{}' → SUCCESS (items={} elapsed={})",
                phaseName,
                status?.itemsProcessed ?: 0,
                formatDuration(elapsed)
            )
        } catch (e: Throwable) {
            phaseTracker.markFailed(phaseName, e.message ?: "Unknown error")
            val elapsed = System.currentTimeMillis() - phaseStartedAt
            log.error("PHASE_TRACKER: '{}' → FAILED after {}: {}", phaseName, formatDuration(elapsed), e.message, e)
            throw e
        }
    }

    private fun resolveCouncilIdForClaim(claim: com.ntcoverage.scraper.ExtractedCouncilClaim): Int? {
        val byKey = councilRepository.findIdByMatchKey(claim.normalizedKey)
        if (byKey != null) return byKey

        val year = claim.claimedYear ?: return null
        return councilRepository.findIdByNameAndYear(claim.councilNameRaw, year)
    }

    private fun normalizeHeresyName(name: String): String =
        name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    private fun linkCouncilFather(councilId: Int, fatherId: Int) {
        transaction {
            val exists = CouncilFathers.selectAll().where {
                (CouncilFathers.councilId eq councilId) and (CouncilFathers.fatherId eq fatherId)
            }.count() > 0
            if (!exists) {
                CouncilFathers.insert {
                    it[CouncilFathers.councilId] = councilId
                    it[CouncilFathers.fatherId] = fatherId
                    it[role] = "attended"
                }
            }
        }
    }

    private suspend fun translateAndSaveCouncilSummary(councilId: Int, displayName: String, summaryEn: String) {
        listOf("pt", "es").forEach { locale ->
            val translated = summarizationService.translateBiography(summaryEn, locale, displayName)
            if (!translated.isNullOrBlank()) {
                val enDisplayName = councilRepository.findById(councilId, "en")?.displayName ?: displayName
                councilRepository.insertOrUpdateTranslation(
                    councilId = councilId,
                    locale = locale,
                    displayName = enDisplayName,
                    summary = translated,
                    translationSource = "machine"
                )
                log.debug("COUNCIL_OVERVIEW_ENRICHMENT: translated summary for councilId={} to {}", councilId, locale)
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}m ${seconds}s"
    }

    private data class SummaryTarget(
        val id: Int,
        val displayName: String,
        val originalText: String?
    )

    private data class CouncilTranslateTarget(
        val id: Int,
        val displayName: String,
        val shortDescription: String?,
        val location: String?,
        val mainTopics: String?,
        val summary: String?
    )

    private data class HeresyTranslateTarget(
        val id: Int,
        val name: String,
        val description: String?
    )

    companion object {
        val ALL_PHASES = listOf(
            "council_seed",
            "council_extract_schaff",
            "council_extract_hefele",
            "council_extract_catholic_enc",
            "council_extract_fordham",
            "council_extract_wikidata",
            "council_extract_wikipedia",
            "council_consensus",
            "council_summaries",
            "council_overview_enrichment",
            "council_translate_all",
            "heresy_translate_all"
        )
    }
}
