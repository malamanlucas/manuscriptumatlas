package com.ntcoverage.scraper

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.LoggerFactory

/**
 * HTTP client for the NTVMR (New Testament Virtual Manuscript Room) API
 * at ntvmr.uni-muenster.de.
 *
 * The NTVMR API returns TEI/XML transcriptions with verse-level granularity,
 * making it the most precise source for which verses a manuscript actually contains.
 *
 * API base: http://ntvmr.uni-muenster.de/community/vmr/api/transcript/get/
 * Parameters:
 *   docID        - 5-digit manuscript ID (1xxxx = papyrus, 2xxxx = uncial)
 *   indexContent - biblical passage (e.g. "John", "John.18")
 *   format       - "teiraw" for TEI XML
 */
class NtvmrClient : AutoCloseable {

    private val log = LoggerFactory.getLogger(NtvmrClient::class.java)
    private val baseUrl = "http://ntvmr.uni-muenster.de/community/vmr/api/transcript/get/"

    companion object {
        val CANONICAL_TO_NTVMR_BOOK = mapOf(
            "Matthew" to "Matt", "Mark" to "Mark", "Luke" to "Luke", "John" to "John",
            "Acts" to "Acts", "Romans" to "Rom",
            "1 Corinthians" to "1Cor", "2 Corinthians" to "2Cor",
            "Galatians" to "Gal", "Ephesians" to "Eph",
            "Philippians" to "Phil", "Colossians" to "Col",
            "1 Thessalonians" to "1Thess", "2 Thessalonians" to "2Thess",
            "1 Timothy" to "1Tim", "2 Timothy" to "2Tim",
            "Titus" to "Tit", "Philemon" to "Phlm",
            "Hebrews" to "Heb", "James" to "Jas",
            "1 Peter" to "1Pet", "2 Peter" to "2Pet",
            "1 John" to "1John", "2 John" to "2John", "3 John" to "3John",
            "Jude" to "Jude", "Revelation" to "Rev"
        )
    }

    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 60_000
        }
    }

    suspend fun fetchTranscript(docId: String, indexContent: String? = null): String? {
        return try {
            val response: HttpResponse = client.get(baseUrl) {
                parameter("docID", docId)
                if (indexContent != null) parameter("indexContent", indexContent)
                parameter("format", "teiraw")
            }
            response.bodyAsText()
        } catch (e: Exception) {
            log.error("Failed to fetch transcript for docID=$docId [${e.javaClass.simpleName}]: ${e.message ?: "no message"}")
            null
        }
    }

    /**
     * Fetches the transcript for a specific book of a manuscript.
     * Uses NTVMR book abbreviations in the indexContent parameter.
     */
    suspend fun fetchBookTranscript(docId: String, canonicalBookName: String): String? {
        val ntvmrBook = CANONICAL_TO_NTVMR_BOOK[canonicalBookName]
        if (ntvmrBook == null) {
            log.warn("No NTVMR mapping for book: $canonicalBookName")
            return null
        }
        return fetchTranscript(docId, ntvmrBook)
    }

    override fun close() {
        client.close()
    }
}
