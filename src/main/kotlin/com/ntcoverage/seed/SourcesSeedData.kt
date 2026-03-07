package com.ntcoverage.seed

data class SourceSeedEntry(
    val name: String,
    val displayName: String,
    val sourceLevel: String,
    val baseWeight: Double,
    val url: String? = null,
    val description: String? = null
)

object SourcesSeedData {
    val entries: List<SourceSeedEntry> = listOf(
        SourceSeedEntry(
            name = "schaff",
            displayName = "Philip Schaff - NPNF2-14 Seven Ecumenical Councils",
            sourceLevel = "PRIMARY",
            baseWeight = 1.0,
            url = "https://ccel.org/ccel/schaff/npnf214",
            description = "Nicene and Post-Nicene Fathers, Series II, Vol. XIV, edited by H. R. Percival."
        ),
        SourceSeedEntry(
            name = "hefele",
            displayName = "Karl Hefele - History of the Councils",
            sourceLevel = "PRIMARY",
            baseWeight = 1.0,
            url = "https://archive.org/details/historyofcouncil03hefeuoft",
            description = "Seven-volume council history from primary documentary sources."
        ),
        SourceSeedEntry(
            name = "fordham",
            displayName = "Fordham Medieval Sourcebook",
            sourceLevel = "PRIMARY",
            baseWeight = 1.0,
            url = "https://sourcebooks.fordham.edu",
            description = "Primary source texts curated by Fordham University Internet History Sourcebooks."
        ),
        SourceSeedEntry(
            name = "catholic_encyclopedia",
            displayName = "Catholic Encyclopedia (1913)",
            sourceLevel = "ACADEMIC",
            baseWeight = 0.8,
            url = "https://www.newadvent.org/cathen/",
            description = "Public-domain scholarly encyclopedia articles on councils and heresies."
        ),
        SourceSeedEntry(
            name = "wikidata",
            displayName = "Wikidata",
            sourceLevel = "STRUCTURED",
            baseWeight = 0.7,
            url = "https://www.wikidata.org",
            description = "Structured linked-data endpoint queried via SPARQL."
        ),
        SourceSeedEntry(
            name = "wikipedia",
            displayName = "Wikipedia (English)",
            sourceLevel = "AGGREGATOR",
            baseWeight = 0.5,
            url = "https://en.wikipedia.org",
            description = "Community-edited encyclopedia used for narrative extraction."
        ),
        SourceSeedEntry(
            name = "biblequery",
            displayName = "BibleQuery Church Councils",
            sourceLevel = "AGGREGATOR",
            baseWeight = 0.5,
            url = "https://biblequery.org/History/ChurchHistory/ChurchCouncils.html",
            description = "Chronological aggregate list used for discovery and gap checks."
        ),
        SourceSeedEntry(
            name = "seed",
            displayName = "Curated Seed Dataset",
            sourceLevel = "ACADEMIC",
            baseWeight = 0.8,
            description = "Project-curated list based on multiple documentary and academic references."
        ),
        SourceSeedEntry(
            name = "ai_enrichment",
            displayName = "AI-Generated Enrichment",
            sourceLevel = "AGGREGATOR",
            baseWeight = 0.3,
            description = "Machine-generated summaries when no other source available. Lower confidence."
        )
    )
}
