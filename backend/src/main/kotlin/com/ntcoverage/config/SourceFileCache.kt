package com.ntcoverage.config

import com.ntcoverage.model.CacheEntryDTO
import com.ntcoverage.model.CacheStatsDTO
import org.slf4j.LoggerFactory
import java.io.File

class SourceFileCache(
    private val cacheDirPath: String = System.getenv("SOURCE_CACHE_DIR") ?: "/data/source-cache"
) {
    private val log = LoggerFactory.getLogger(SourceFileCache::class.java)
    private val cacheDir = File(cacheDirPath).apply { mkdirs() }

    fun has(key: String): Boolean = resolveFile(key).exists()

    fun get(key: String): String? {
        val file = resolveFile(key)
        return if (file.exists()) file.readText() else null
    }

    fun put(key: String, content: String) {
        val file = resolveFile(key)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    suspend fun getOrFetch(key: String, fetcher: suspend () -> String): String {
        get(key)?.let { cached ->
            log.debug("SOURCE_CACHE_HIT key={}", key)
            return cached
        }

        log.info("SOURCE_CACHE_MISS key={} - downloading", key)
        val fetched = fetcher()
        put(key, fetched)
        return fetched
    }

    fun clear(): Int {
        if (!cacheDir.exists()) return 0
        val files = cacheDir.walkTopDown().filter { it.isFile }.toList()
        files.forEach { it.delete() }
        return files.size
    }

    fun getStats(): CacheStatsDTO {
        if (!cacheDir.exists()) {
            return CacheStatsDTO(totalFiles = 0, totalSizeBytes = 0, totalSizeMb = 0.0, entries = emptyList())
        }

        val files = cacheDir.walkTopDown()
            .filter { it.isFile }
            .toList()

        val entries = files
            .map { file ->
                val key = file.relativeTo(cacheDir).invariantSeparatorsPath
                CacheEntryDTO(key = key, sizeBytes = file.length())
            }
            .sortedBy { it.key }

        val totalSizeBytes = entries.sumOf { it.sizeBytes }
        val totalSizeMb = totalSizeBytes.toDouble() / (1024.0 * 1024.0)

        return CacheStatsDTO(
            totalFiles = entries.size,
            totalSizeBytes = totalSizeBytes,
            totalSizeMb = String.format("%.2f", totalSizeMb).toDouble(),
            entries = entries
        )
    }

    private fun resolveFile(key: String): File {
        val normalizedKey = key.trimStart('/').replace("..", "")
        return File(cacheDir, normalizedKey)
    }
}
