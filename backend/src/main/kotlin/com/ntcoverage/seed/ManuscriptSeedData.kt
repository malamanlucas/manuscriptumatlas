package com.ntcoverage.seed

import com.ntcoverage.model.ManuscriptSeed
import kotlinx.serialization.json.Json

object ManuscriptSeedData {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<ManuscriptSeed> {
        val stream = this::class.java.classLoader.getResourceAsStream("seed/manuscripts.json")
            ?: throw IllegalStateException("manuscripts.json not found in resources/seed/")
        val text = stream.bufferedReader().use { it.readText() }
        return json.decodeFromString<List<ManuscriptSeed>>(text)
    }
}
