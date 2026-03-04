package com.ntcoverage.scraper

import com.ntcoverage.model.ManuscriptSeed
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Fetches the manuscript catalog from NTVMR metadata/liste/search API.
 * Used when LOAD_MANUSCRIPTS_FROM_NTVMR=true to load all papyri (1xxxx) and uncials (2xxxx)
 * instead of the curated seed JSON.
 */
class NtvmrListClient : AutoCloseable {

    private val log = LoggerFactory.getLogger(NtvmrListClient::class.java)
    private val listBaseUrl = "http://ntvmr.uni-muenster.de/community/vmr/api/metadata/liste/search/"
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 60_000
        }
    }

    /**
     * Fetches all NT manuscripts (papyri + uncials) from the NTVMR catalog.
     * Papyri: docID 10000-19999, Uncials: docID 20000-29999.
     * Returns ManuscriptSeed with content=emptyList(); verse data will be fetched via transcript API.
     */
    suspend fun fetchManuscripts(): List<ManuscriptSeed> {
        val papyri = fetchRange("10000-19999", "papyrus")
        val uncials = fetchRange("20000-29999", "uncial")
        val all = papyri + uncials
        log.info("NTVMR list: fetched ${papyri.size} papyri + ${uncials.size} uncials = ${all.size} manuscripts")
        return all
    }

    private suspend fun fetchRange(docIdRange: String, type: String): List<ManuscriptSeed> {
        val allResults = mutableListOf<ManuscriptSeed>()

        try {
            val response = client.get(listBaseUrl) {
                parameter("docID", docIdRange)
                parameter("format", "json")
                parameter("detail", "document")
            }
            val body = response.bodyAsText()
            val parsed = json.decodeFromString<NtvmrListResponse>(body)
            val list = parsed.data?.manuscripts?.manuscript ?: emptyList()

            val page = list.mapNotNull { doc ->
                val centuryMin = yearToCentury(doc.origEarly)
                val centuryMax = yearToCentury(doc.origLate)
                val rawGaId = doc.gaNum.content
                val gaId = if (type == "uncial" && rawGaId.firstOrNull()?.isDigit() == true && !rawGaId.startsWith("0")) {
                    "0$rawGaId"
                } else {
                    rawGaId
                }
                ManuscriptSeed(
                    gaId = gaId,
                    name = "GA $gaId",
                    centuryMin = centuryMin,
                    centuryMax = centuryMax,
                    type = type,
                    content = emptyList(),
                    yearMin = doc.origEarly.takeIf { it > 0 },
                    yearMax = doc.origLate.takeIf { it > 0 }
                )
            }
            allResults.addAll(page)
        } catch (e: Exception) {
            log.error("Failed to fetch NTVMR list for $type (docID=$docIdRange): ${e.message}")
        }

        return allResults
    }

    /** Converts NTVMR year (e.g. 200, 599) to century 1-10. */
    private fun yearToCentury(year: Int): Int {
        if (year <= 0) return 1
        val c = ((year - 1) / 100) + 1
        return maxOf(1, minOf(10, c))
    }

    override fun close() {
        client.close()
    }

    @Serializable
    private data class NtvmrListResponse(
        val status: String? = null,
        val data: NtvmrListData? = null
    )

    @Serializable
    private data class NtvmrListData(
        val manuscripts: NtvmrManuscripts? = null
    )

    @Serializable
    private data class NtvmrManuscripts(
        val manuscript: List<NtvmrManuscriptEntry> = emptyList(),
        val count: Int? = null
    )

    @Serializable
    private data class NtvmrManuscriptEntry(
        val docID: Int,
        val gaNum: JsonPrimitive,
        val origEarly: Int = 0,
        val origLate: Int = 0,
        val orig: String? = null
    )
}
