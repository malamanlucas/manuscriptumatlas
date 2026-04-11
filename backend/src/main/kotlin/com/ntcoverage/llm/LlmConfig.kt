package com.ntcoverage.llm

class LlmConfig {
    // ══════════════════════════════════════════════════════════════════
    // OpenAI API (primary and only provider)
    // ══════════════════════════════════════════════════════════════════
    val openaiApiKey: String? = System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }

    // Per-tier model selection
    val openaiLowModel: String = System.getenv("OPENAI_LOW_MODEL") ?: "gpt-4.1-mini"
    val openaiMediumModel: String = System.getenv("OPENAI_MEDIUM_MODEL") ?: "gpt-4.1"
    val openaiHighModel: String = System.getenv("OPENAI_HIGH_MODEL") ?: "gpt-4.1"

    // Per-tier concurrency
    val lowConcurrency: Int = System.getenv("LLM_LOW_CONCURRENCY")?.toIntOrNull() ?: 80
    val mediumConcurrency: Int = System.getenv("LLM_MEDIUM_CONCURRENCY")?.toIntOrNull() ?: 40
    val highConcurrency: Int = System.getenv("LLM_HIGH_CONCURRENCY")?.toIntOrNull() ?: 15

    // Per-tier batch sizes
    val lowBatchSize: Int = System.getenv("LLM_LOW_BATCH_SIZE")?.toIntOrNull() ?: 150
    val mediumBatchSize: Int = System.getenv("LLM_MEDIUM_BATCH_SIZE")?.toIntOrNull() ?: 80
    val highBatchSize: Int = System.getenv("LLM_HIGH_BATCH_SIZE")?.toIntOrNull() ?: 20

    // Per-tier timeouts (ms)
    val lowTimeoutMs: Long = System.getenv("LLM_LOW_TIMEOUT_MS")?.toLongOrNull() ?: 30_000L
    val mediumTimeoutMs: Long = System.getenv("LLM_MEDIUM_TIMEOUT_MS")?.toLongOrNull() ?: 60_000L
    val highTimeoutMs: Long = System.getenv("LLM_HIGH_TIMEOUT_MS")?.toLongOrNull() ?: 120_000L

    // Generic delays and concurrency
    val alignmentDelayMs: Long = System.getenv("LLM_ALIGNMENT_DELAY_MS")?.toLongOrNull() ?: 50L
    val enrichmentDelayMs: Long = System.getenv("LLM_ENRICHMENT_DELAY_MS")?.toLongOrNull() ?: 100L
    val concurrency: Int = System.getenv("LLM_CONCURRENCY")?.toIntOrNull() ?: 15

}
