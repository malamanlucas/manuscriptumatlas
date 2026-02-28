package com.ntcoverage.config

object IngestionConfig {
    val enableIngestion: Boolean
        get() = System.getenv("ENABLE_INGESTION")?.lowercase() != "false"

    val skipIfPopulated: Boolean
        get() = System.getenv("INGESTION_SKIP_IF_POPULATED")?.lowercase() == "true"

    val timeoutMinutes: Long
        get() = System.getenv("INGESTION_TIMEOUT_MINUTES")?.toLongOrNull() ?: 30L

    /** When true, load manuscript list from NTVMR API (100+ papyri + uncials) instead of seed JSON. For DEV. */
    val loadManuscriptsFromNtvmr: Boolean
        get() = System.getenv("LOAD_MANUSCRIPTS_FROM_NTVMR")?.lowercase() == "true"

    val enableManuscriptIngestion: Boolean
        get() = System.getenv("ENABLE_MANUSCRIPT_INGESTION")?.lowercase() != "false"

    val enablePatristicIngestion: Boolean
        get() = System.getenv("ENABLE_PATRISTIC_INGESTION")?.lowercase() != "false"
}
