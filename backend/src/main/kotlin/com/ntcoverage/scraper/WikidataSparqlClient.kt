package com.ntcoverage.scraper

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

class WikidataSparqlClient : CouncilSourceExtractor {
    override val sourceName: String = "wikidata"
    private val log = LoggerFactory.getLogger(WikidataSparqlClient::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(CIO) {
        engine { requestTimeout = 30_000 }
    }

    private val sparqlQuery = """
        SELECT ?council ?councilLabel ?coord ?lat ?lon ?date ?startDate ?endDate ?numParticipants
        WHERE {
            ?council wdt:P31/wdt:P279* wd:Q1371649 .
            OPTIONAL { ?council wdt:P585 ?date . }
            OPTIONAL { ?council wdt:P580 ?startDate . }
            OPTIONAL { ?council wdt:P582 ?endDate . }
            OPTIONAL { ?council p:P625/psv:P625 [ wikibase:geoLatitude ?lat; wikibase:geoLongitude ?lon ] . }
            OPTIONAL { ?council wdt:P625 ?coord . }
            OPTIONAL { ?council wdt:P1132 ?numParticipants . }
            FILTER(
                (BOUND(?date) && YEAR(?date) <= 1000) ||
                (BOUND(?startDate) && YEAR(?startDate) <= 1000)
            )
            SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
        }
    """.trimIndent()

    override suspend fun extract(): List<ExtractedCouncilClaim> {
        log.info("WIKIDATA_EXTRACTOR: starting SPARQL query")
        val response: HttpResponse = client.get("https://query.wikidata.org/sparql") {
            parameter("query", sparqlQuery)
            parameter("format", "json")
            header(HttpHeaders.Accept, "application/sparql-results+json")
            header(HttpHeaders.UserAgent, "ManuscriptumAtlasBot/1.0 (historical research)")
        }
        val body = response.bodyAsText()
        log.info("WIKIDATA_EXTRACTOR: response status={} bodyLength={}", response.status, body.length)
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Wikidata SPARQL failed: status=${response.status}")
        }

        val root = json.parseToJsonElement(body).jsonObject
        val rows = root["results"]?.jsonObject?.get("bindings")?.jsonArray ?: return emptyList()
        log.info("WIKIDATA_EXTRACTOR: SPARQL returned {} rows", rows.size)
        val aggregated = linkedMapOf<String, MutableMap<String, String?>>()

        for ((idx, row) in rows.withIndex()) {
            val obj = row.jsonObject
            val id = obj["council"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: continue
            val council = aggregated.getOrPut(id) { mutableMapOf() }
            council["label"] = obj["councilLabel"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: council["label"]
            council["date"] = obj["date"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: council["date"]
            council["startDate"] = obj["startDate"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: council["startDate"]
            council["endDate"] = obj["endDate"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: council["endDate"]
            council["lat"] = obj["lat"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: council["lat"]
            council["lon"] = obj["lon"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: council["lon"]
            council["participants"] = obj["numParticipants"]?.jsonObject?.get("value")?.jsonPrimitive?.content ?: council["participants"]
            if ((idx + 1) % 100 == 0 || idx == rows.lastIndex) {
                val pct = if (rows.isNotEmpty()) ((idx + 1) * 100) / rows.size else 100
                log.info(
                    "WIKIDATA_EXTRACTOR: row progress {}/{} ({}%) uniqueCouncils={}",
                    idx + 1, rows.size, pct, aggregated.size
                )
            }
        }

        val claims = aggregated.values.mapNotNull { row ->
            val name = row["label"] ?: return@mapNotNull null
            val year = parseYear(row["startDate"] ?: row["date"])
            val yearEnd = parseYear(row["endDate"])
            ExtractedCouncilClaim(
                councilNameRaw = name,
                normalizedKey = year?.let { CouncilNameNormalizer.matchKey(name, it) }
                    ?: CouncilNameNormalizer.normalize(name),
                claimedYear = year,
                claimedYearEnd = yearEnd,
                claimedLocation = null,
                claimedParticipants = row["participants"]?.toDoubleOrNull()?.toInt(),
                rawText = "lat=${row["lat"]}; lon=${row["lon"]}; id=wikidata",
                sourcePage = "https://www.wikidata.org/wiki/${name.replace(' ', '_')}"
            )
        }

        log.info("WIKIDATA_EXTRACTOR: aggregated {} councils and extracted {} claims", aggregated.size, claims.size)
        return claims
    }

    private fun parseYear(value: String?): Int? {
        if (value == null) return null
        val normalized = value.trim().removePrefix("+")
        return Regex("""(\d{3,4})""").find(normalized)?.groupValues?.get(1)?.toIntOrNull()
    }
}
