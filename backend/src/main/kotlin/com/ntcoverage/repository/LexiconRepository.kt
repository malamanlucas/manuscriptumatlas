package com.ntcoverage.repository

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class LexiconRepository {

    private val db get() = BibleDatabaseConfig.database

    fun upsertGreek(
        strongsNumber: String, lemma: String, transliteration: String?,
        pronunciation: String?, shortDefinition: String?, fullDefinition: String?,
        partOfSpeech: String?, usageCount: Int
    ) = transaction(db) {
        val existing = GreekLexicon.selectAll()
            .where { GreekLexicon.strongsNumber eq strongsNumber }
            .firstOrNull()
        if (existing == null) {
            GreekLexicon.insert {
                it[GreekLexicon.strongsNumber] = strongsNumber
                it[GreekLexicon.lemma] = lemma
                it[GreekLexicon.transliteration] = transliteration
                it[GreekLexicon.pronunciation] = pronunciation
                it[GreekLexicon.shortDefinition] = shortDefinition
                it[GreekLexicon.fullDefinition] = fullDefinition
                it[GreekLexicon.partOfSpeech] = partOfSpeech
                it[GreekLexicon.usageCount] = usageCount
            }
        }
    }

    fun upsertHebrew(
        strongsNumber: String, lemma: String, transliteration: String?,
        pronunciation: String?, shortDefinition: String?, fullDefinition: String?,
        partOfSpeech: String?, usageCount: Int
    ) = transaction(db) {
        val existing = HebrewLexicon.selectAll()
            .where { HebrewLexicon.strongsNumber eq strongsNumber }
            .firstOrNull()
        if (existing == null) {
            HebrewLexicon.insert {
                it[HebrewLexicon.strongsNumber] = strongsNumber
                it[HebrewLexicon.lemma] = lemma
                it[HebrewLexicon.transliteration] = transliteration
                it[HebrewLexicon.pronunciation] = pronunciation
                it[HebrewLexicon.shortDefinition] = shortDefinition
                it[HebrewLexicon.fullDefinition] = fullDefinition
                it[HebrewLexicon.partOfSpeech] = partOfSpeech
                it[HebrewLexicon.usageCount] = usageCount
            }
        }
    }

    private fun ResultRow.toGreekDTO() = LexiconEntryDTO(
        id = this[GreekLexicon.id].value,
        strongsNumber = this[GreekLexicon.strongsNumber],
        lemma = this[GreekLexicon.lemma],
        transliteration = this[GreekLexicon.transliteration],
        pronunciation = this[GreekLexicon.pronunciation],
        shortDefinition = this[GreekLexicon.shortDefinition],
        fullDefinition = this[GreekLexicon.fullDefinition],
        partOfSpeech = this[GreekLexicon.partOfSpeech],
        usageCount = this[GreekLexicon.usageCount],
        language = "greek",
        phoneticSpelling = this[GreekLexicon.phoneticSpelling],
        kjvTranslation = this[GreekLexicon.kjvTranslation],
        kjvUsageCount = this[GreekLexicon.kjvUsageCount],
        nasbTranslation = this[GreekLexicon.nasbTranslation],
        wordOrigin = this[GreekLexicon.wordOrigin],
        strongsExhaustive = this[GreekLexicon.strongsExhaustive],
        nasExhaustiveOrigin = this[GreekLexicon.nasExhaustiveOrigin],
        nasExhaustiveDefinition = this[GreekLexicon.nasExhaustiveDefinition],
        nasExhaustiveTranslation = this[GreekLexicon.nasExhaustiveTranslation]
    )

    private fun ResultRow.toHebrewDTO() = LexiconEntryDTO(
        id = this[HebrewLexicon.id].value,
        strongsNumber = this[HebrewLexicon.strongsNumber],
        lemma = this[HebrewLexicon.lemma],
        transliteration = this[HebrewLexicon.transliteration],
        pronunciation = this[HebrewLexicon.pronunciation],
        shortDefinition = this[HebrewLexicon.shortDefinition],
        fullDefinition = this[HebrewLexicon.fullDefinition],
        partOfSpeech = this[HebrewLexicon.partOfSpeech],
        usageCount = this[HebrewLexicon.usageCount],
        language = "hebrew",
        phoneticSpelling = this[HebrewLexicon.phoneticSpelling],
        kjvTranslation = this[HebrewLexicon.kjvTranslation],
        kjvUsageCount = this[HebrewLexicon.kjvUsageCount],
        nasbTranslation = this[HebrewLexicon.nasbTranslation],
        wordOrigin = this[HebrewLexicon.wordOrigin],
        strongsExhaustive = this[HebrewLexicon.strongsExhaustive],
        nasExhaustiveOrigin = this[HebrewLexicon.nasExhaustiveOrigin],
        nasExhaustiveDefinition = this[HebrewLexicon.nasExhaustiveDefinition],
        nasExhaustiveTranslation = this[HebrewLexicon.nasExhaustiveTranslation]
    )

    fun enrichHebrewEntry(
        strongsNumber: String, pronunciation: String?, phoneticSpelling: String?,
        kjvTranslation: String?, kjvUsageCount: Int?, nasbTranslation: String?,
        wordOrigin: String?, strongsExhaustive: String?,
        nasExhaustiveOrigin: String?, nasExhaustiveDefinition: String?, nasExhaustiveTranslation: String?,
        shortDefinition: String? = null,
        sourceUrl: String? = null
    ) = transaction(db) {
        HebrewLexicon.update({ HebrewLexicon.strongsNumber eq strongsNumber }) {
            if (sourceUrl != null) it[HebrewLexicon.sourceUrl] = sourceUrl
            if (pronunciation != null) it[HebrewLexicon.pronunciation] = pronunciation
            if (phoneticSpelling != null) it[HebrewLexicon.phoneticSpelling] = phoneticSpelling
            if (kjvTranslation != null) it[HebrewLexicon.kjvTranslation] = kjvTranslation
            if (kjvUsageCount != null) it[HebrewLexicon.kjvUsageCount] = kjvUsageCount
            if (nasbTranslation != null) it[HebrewLexicon.nasbTranslation] = nasbTranslation
            if (wordOrigin != null) it[HebrewLexicon.wordOrigin] = wordOrigin
            if (strongsExhaustive != null) it[HebrewLexicon.strongsExhaustive] = strongsExhaustive
            if (nasExhaustiveOrigin != null) it[HebrewLexicon.nasExhaustiveOrigin] = nasExhaustiveOrigin
            if (nasExhaustiveDefinition != null) it[HebrewLexicon.nasExhaustiveDefinition] = nasExhaustiveDefinition
            if (nasExhaustiveTranslation != null) it[HebrewLexicon.nasExhaustiveTranslation] = nasExhaustiveTranslation
            if (shortDefinition != null) it[HebrewLexicon.shortDefinition] = shortDefinition
        }
    }

    fun isHebrewEntryEnriched(strongsNumber: String): Boolean = transaction(db) {
        HebrewLexicon.selectAll()
            .where { (HebrewLexicon.strongsNumber eq strongsNumber) and (HebrewLexicon.phoneticSpelling.isNotNull()) }
            .count() > 0
    }

    fun enrichGreekEntry(
        strongsNumber: String, pronunciation: String?, phoneticSpelling: String?,
        kjvTranslation: String?, kjvUsageCount: Int?, nasbTranslation: String?,
        wordOrigin: String?, strongsExhaustive: String?,
        nasExhaustiveOrigin: String?, nasExhaustiveDefinition: String?, nasExhaustiveTranslation: String?,
        shortDefinition: String? = null,
        sourceUrl: String? = null
    ) = transaction(db) {
        GreekLexicon.update({ GreekLexicon.strongsNumber eq strongsNumber }) {
            if (sourceUrl != null) it[GreekLexicon.sourceUrl] = sourceUrl
            if (pronunciation != null) it[GreekLexicon.pronunciation] = pronunciation
            if (phoneticSpelling != null) it[GreekLexicon.phoneticSpelling] = phoneticSpelling
            if (kjvTranslation != null) it[GreekLexicon.kjvTranslation] = kjvTranslation
            if (kjvUsageCount != null) it[GreekLexicon.kjvUsageCount] = kjvUsageCount
            if (nasbTranslation != null) it[GreekLexicon.nasbTranslation] = nasbTranslation
            if (wordOrigin != null) it[GreekLexicon.wordOrigin] = wordOrigin
            if (strongsExhaustive != null) it[GreekLexicon.strongsExhaustive] = strongsExhaustive
            if (nasExhaustiveOrigin != null) it[GreekLexicon.nasExhaustiveOrigin] = nasExhaustiveOrigin
            if (nasExhaustiveDefinition != null) it[GreekLexicon.nasExhaustiveDefinition] = nasExhaustiveDefinition
            if (nasExhaustiveTranslation != null) it[GreekLexicon.nasExhaustiveTranslation] = nasExhaustiveTranslation
            if (shortDefinition != null) it[GreekLexicon.shortDefinition] = shortDefinition
        }
    }

    fun isGreekEntryEnriched(strongsNumber: String): Boolean = transaction(db) {
        GreekLexicon.selectAll()
            .where { (GreekLexicon.strongsNumber eq strongsNumber) and (GreekLexicon.phoneticSpelling.isNotNull()) }
            .count() > 0
    }

    fun findByStrongsNumber(strongsNumber: String): LexiconEntryDTO? = transaction(db) {
        val sn = strongsNumber.uppercase()
        if (sn.startsWith("G")) {
            GreekLexicon.selectAll()
                .where { GreekLexicon.strongsNumber eq sn }
                .firstOrNull()?.toGreekDTO()
        } else if (sn.startsWith("H")) {
            HebrewLexicon.selectAll()
                .where { HebrewLexicon.strongsNumber eq sn }
                .firstOrNull()?.toHebrewDTO()
        } else null
    }

    fun countGreek(): Long = transaction(db) { GreekLexicon.selectAll().count() }
    fun countHebrew(): Long = transaction(db) { HebrewLexicon.selectAll().count() }

    fun getAllGreekEntries(): List<LexiconEntryDTO> = transaction(db) {
        GreekLexicon.selectAll().map { it.toGreekDTO() }
    }

    fun upsertGreekTranslation(lexiconId: Int, locale: String, shortDef: String?, fullDef: String?) = transaction(db) {
        val existing = GreekLexiconTranslations.selectAll()
            .where { (GreekLexiconTranslations.lexiconId eq lexiconId) and (GreekLexiconTranslations.locale eq locale) }
            .firstOrNull()
        if (existing == null) {
            GreekLexiconTranslations.insert {
                it[GreekLexiconTranslations.lexiconId] = lexiconId
                it[GreekLexiconTranslations.locale] = locale
                it[GreekLexiconTranslations.shortDefinition] = shortDef
                it[GreekLexiconTranslations.fullDefinition] = fullDef
            }
        } else {
            GreekLexiconTranslations.update({
                (GreekLexiconTranslations.lexiconId eq lexiconId) and (GreekLexiconTranslations.locale eq locale)
            }) {
                if (shortDef != null) it[GreekLexiconTranslations.shortDefinition] = shortDef
                if (fullDef != null) it[GreekLexiconTranslations.fullDefinition] = fullDef
            }
        }
    }

    fun hasTranslation(lexiconId: Int, locale: String): Boolean = transaction(db) {
        GreekLexiconTranslations.selectAll()
            .where {
                (GreekLexiconTranslations.lexiconId eq lexiconId) and
                (GreekLexiconTranslations.locale eq locale) and
                (GreekLexiconTranslations.shortDefinition.isNotNull())
            }
            .count() > 0
    }

    fun upsertGreekEnrichmentTranslation(
        lexiconId: Int, locale: String,
        kjvTranslation: String?, wordOrigin: String?, strongsExhaustive: String?,
        nasExhaustiveOrigin: String?, nasExhaustiveDefinition: String?, nasExhaustiveTranslation: String?
    ) = transaction(db) {
        val existing = GreekLexiconTranslations.selectAll()
            .where { (GreekLexiconTranslations.lexiconId eq lexiconId) and (GreekLexiconTranslations.locale eq locale) }
            .firstOrNull()
        if (existing == null) {
            GreekLexiconTranslations.insert {
                it[GreekLexiconTranslations.lexiconId] = lexiconId
                it[GreekLexiconTranslations.locale] = locale
                if (kjvTranslation != null) it[GreekLexiconTranslations.kjvTranslation] = kjvTranslation
                if (wordOrigin != null) it[GreekLexiconTranslations.wordOrigin] = wordOrigin
                if (strongsExhaustive != null) it[GreekLexiconTranslations.strongsExhaustive] = strongsExhaustive
                if (nasExhaustiveOrigin != null) it[GreekLexiconTranslations.nasExhaustiveOrigin] = nasExhaustiveOrigin
                if (nasExhaustiveDefinition != null) it[GreekLexiconTranslations.nasExhaustiveDefinition] = nasExhaustiveDefinition
                if (nasExhaustiveTranslation != null) it[GreekLexiconTranslations.nasExhaustiveTranslation] = nasExhaustiveTranslation
            }
        } else {
            GreekLexiconTranslations.update({
                (GreekLexiconTranslations.lexiconId eq lexiconId) and (GreekLexiconTranslations.locale eq locale)
            }) {
                if (kjvTranslation != null) it[GreekLexiconTranslations.kjvTranslation] = kjvTranslation
                if (wordOrigin != null) it[GreekLexiconTranslations.wordOrigin] = wordOrigin
                if (strongsExhaustive != null) it[GreekLexiconTranslations.strongsExhaustive] = strongsExhaustive
                if (nasExhaustiveOrigin != null) it[GreekLexiconTranslations.nasExhaustiveOrigin] = nasExhaustiveOrigin
                if (nasExhaustiveDefinition != null) it[GreekLexiconTranslations.nasExhaustiveDefinition] = nasExhaustiveDefinition
                if (nasExhaustiveTranslation != null) it[GreekLexiconTranslations.nasExhaustiveTranslation] = nasExhaustiveTranslation
            }
        }
    }

    fun hasGreekEnrichmentTranslation(lexiconId: Int, locale: String): Boolean = transaction(db) {
        GreekLexiconTranslations.selectAll()
            .where {
                (GreekLexiconTranslations.lexiconId eq lexiconId) and
                (GreekLexiconTranslations.locale eq locale) and
                (GreekLexiconTranslations.wordOrigin.isNotNull())
            }
            .count() > 0
    }

    fun upsertHebrewEnrichmentTranslation(
        lexiconId: Int, locale: String,
        kjvTranslation: String?, wordOrigin: String?, strongsExhaustive: String?,
        nasExhaustiveOrigin: String?, nasExhaustiveDefinition: String?, nasExhaustiveTranslation: String?
    ) = transaction(db) {
        val existing = HebrewLexiconTranslations.selectAll()
            .where { (HebrewLexiconTranslations.lexiconId eq lexiconId) and (HebrewLexiconTranslations.locale eq locale) }
            .firstOrNull()
        if (existing == null) {
            HebrewLexiconTranslations.insert {
                it[HebrewLexiconTranslations.lexiconId] = lexiconId
                it[HebrewLexiconTranslations.locale] = locale
                if (kjvTranslation != null) it[HebrewLexiconTranslations.kjvTranslation] = kjvTranslation
                if (wordOrigin != null) it[HebrewLexiconTranslations.wordOrigin] = wordOrigin
                if (strongsExhaustive != null) it[HebrewLexiconTranslations.strongsExhaustive] = strongsExhaustive
                if (nasExhaustiveOrigin != null) it[HebrewLexiconTranslations.nasExhaustiveOrigin] = nasExhaustiveOrigin
                if (nasExhaustiveDefinition != null) it[HebrewLexiconTranslations.nasExhaustiveDefinition] = nasExhaustiveDefinition
                if (nasExhaustiveTranslation != null) it[HebrewLexiconTranslations.nasExhaustiveTranslation] = nasExhaustiveTranslation
            }
        } else {
            HebrewLexiconTranslations.update({
                (HebrewLexiconTranslations.lexiconId eq lexiconId) and (HebrewLexiconTranslations.locale eq locale)
            }) {
                if (kjvTranslation != null) it[HebrewLexiconTranslations.kjvTranslation] = kjvTranslation
                if (wordOrigin != null) it[HebrewLexiconTranslations.wordOrigin] = wordOrigin
                if (strongsExhaustive != null) it[HebrewLexiconTranslations.strongsExhaustive] = strongsExhaustive
                if (nasExhaustiveOrigin != null) it[HebrewLexiconTranslations.nasExhaustiveOrigin] = nasExhaustiveOrigin
                if (nasExhaustiveDefinition != null) it[HebrewLexiconTranslations.nasExhaustiveDefinition] = nasExhaustiveDefinition
                if (nasExhaustiveTranslation != null) it[HebrewLexiconTranslations.nasExhaustiveTranslation] = nasExhaustiveTranslation
            }
        }
    }

    fun hasHebrewEnrichmentTranslation(lexiconId: Int, locale: String): Boolean = transaction(db) {
        HebrewLexiconTranslations.selectAll()
            .where {
                (HebrewLexiconTranslations.lexiconId eq lexiconId) and
                (HebrewLexiconTranslations.locale eq locale) and
                (HebrewLexiconTranslations.wordOrigin.isNotNull())
            }
            .count() > 0
    }

    fun getAllHebrewEntries(): List<LexiconEntryDTO> = transaction(db) {
        HebrewLexicon.selectAll().map { it.toHebrewDTO() }
    }

    fun hasHebrewTranslation(lexiconId: Int, locale: String): Boolean = transaction(db) {
        HebrewLexiconTranslations.selectAll()
            .where {
                (HebrewLexiconTranslations.lexiconId eq lexiconId) and
                (HebrewLexiconTranslations.locale eq locale) and
                (HebrewLexiconTranslations.shortDefinition.isNotNull())
            }
            .count() > 0
    }

    fun upsertHebrewTranslation(lexiconId: Int, locale: String, shortDef: String?, fullDef: String?) = transaction(db) {
        val existing = HebrewLexiconTranslations.selectAll()
            .where { (HebrewLexiconTranslations.lexiconId eq lexiconId) and (HebrewLexiconTranslations.locale eq locale) }
            .firstOrNull()
        if (existing == null) {
            HebrewLexiconTranslations.insert {
                it[HebrewLexiconTranslations.lexiconId] = lexiconId
                it[HebrewLexiconTranslations.locale] = locale
                it[HebrewLexiconTranslations.shortDefinition] = shortDef
                it[HebrewLexiconTranslations.fullDefinition] = fullDef
            }
        } else {
            HebrewLexiconTranslations.update({
                (HebrewLexiconTranslations.lexiconId eq lexiconId) and (HebrewLexiconTranslations.locale eq locale)
            }) {
                if (shortDef != null) it[HebrewLexiconTranslations.shortDefinition] = shortDef
                if (fullDef != null) it[HebrewLexiconTranslations.fullDefinition] = fullDef
            }
        }
    }

    fun batchUpsertHebrewTranslations(entries: List<Triple<Int, String?, String?>>, locale: String) = transaction(db) {
        for ((lexiconId, shortDef, fullDef) in entries) {
            val existing = HebrewLexiconTranslations.selectAll()
                .where { (HebrewLexiconTranslations.lexiconId eq lexiconId) and (HebrewLexiconTranslations.locale eq locale) }
                .firstOrNull()
            if (existing == null) {
                HebrewLexiconTranslations.insert {
                    it[HebrewLexiconTranslations.lexiconId] = lexiconId
                    it[HebrewLexiconTranslations.locale] = locale
                    it[HebrewLexiconTranslations.shortDefinition] = shortDef
                    it[HebrewLexiconTranslations.fullDefinition] = fullDef
                }
            } else {
                HebrewLexiconTranslations.update({
                    (HebrewLexiconTranslations.lexiconId eq lexiconId) and (HebrewLexiconTranslations.locale eq locale)
                }) {
                    if (shortDef != null) it[HebrewLexiconTranslations.shortDefinition] = shortDef
                    if (fullDef != null) it[HebrewLexiconTranslations.fullDefinition] = fullDef
                }
            }
        }
    }

    fun getExistingHebrewStrongsNumbers(): Set<String> = transaction(db) {
        HebrewLexicon.select(HebrewLexicon.strongsNumber)
            .map { it[HebrewLexicon.strongsNumber] }
            .toSet()
    }

    fun insertHebrewFromBibleHub(
        strongsNumber: String, lemma: String, transliteration: String?,
        pronunciation: String?, shortDefinition: String?, fullDefinition: String?,
        partOfSpeech: String?, phoneticSpelling: String?,
        kjvTranslation: String?, kjvUsageCount: Int?, nasbTranslation: String?,
        wordOrigin: String?, strongsExhaustive: String?,
        nasExhaustiveOrigin: String?, nasExhaustiveDefinition: String?,
        nasExhaustiveTranslation: String?, sourceUrl: String?
    ) = transaction(db) {
        val existing = HebrewLexicon.selectAll()
            .where { HebrewLexicon.strongsNumber eq strongsNumber }
            .firstOrNull()
        if (existing == null) {
            HebrewLexicon.insert {
                it[HebrewLexicon.strongsNumber] = strongsNumber
                it[HebrewLexicon.lemma] = lemma
                it[HebrewLexicon.transliteration] = transliteration
                it[HebrewLexicon.pronunciation] = pronunciation
                it[HebrewLexicon.shortDefinition] = shortDefinition
                it[HebrewLexicon.fullDefinition] = fullDefinition
                it[HebrewLexicon.partOfSpeech] = partOfSpeech
                it[HebrewLexicon.usageCount] = 0
                it[HebrewLexicon.phoneticSpelling] = phoneticSpelling
                it[HebrewLexicon.kjvTranslation] = kjvTranslation
                it[HebrewLexicon.kjvUsageCount] = kjvUsageCount
                it[HebrewLexicon.nasbTranslation] = nasbTranslation
                it[HebrewLexicon.wordOrigin] = wordOrigin
                it[HebrewLexicon.strongsExhaustive] = strongsExhaustive
                it[HebrewLexicon.nasExhaustiveOrigin] = nasExhaustiveOrigin
                it[HebrewLexicon.nasExhaustiveDefinition] = nasExhaustiveDefinition
                it[HebrewLexicon.nasExhaustiveTranslation] = nasExhaustiveTranslation
                if (sourceUrl != null) it[HebrewLexicon.sourceUrl] = sourceUrl
            }
        }
    }

    fun findByStrongsNumberWithTranslation(strongsNumber: String, locale: String): LexiconEntryDTO? = transaction(db) {
        val sn = strongsNumber.uppercase()
        if (sn.startsWith("G")) {
            findGreekWithTranslation(sn, locale)
        } else if (sn.startsWith("H")) {
            findHebrewWithTranslation(sn, locale)
        } else {
            findByStrongsNumber(strongsNumber)
        }
    }

    private fun findGreekWithTranslation(sn: String, locale: String): LexiconEntryDTO? {
        val entry = GreekLexicon.selectAll()
            .where { GreekLexicon.strongsNumber eq sn }
            .firstOrNull() ?: return null

        val base = entry.toGreekDTO()

        if (locale != "en") {
            val translation = GreekLexiconTranslations.selectAll()
                .where { (GreekLexiconTranslations.lexiconId eq base.id) and (GreekLexiconTranslations.locale eq locale) }
                .firstOrNull()

            if (translation != null) {
                return base.copy(
                    shortDefinition = translation[GreekLexiconTranslations.shortDefinition] ?: base.shortDefinition,
                    fullDefinition = translation[GreekLexiconTranslations.fullDefinition] ?: base.fullDefinition,
                    kjvTranslation = translation[GreekLexiconTranslations.kjvTranslation] ?: base.kjvTranslation,
                    wordOrigin = translation[GreekLexiconTranslations.wordOrigin] ?: base.wordOrigin,
                    strongsExhaustive = translation[GreekLexiconTranslations.strongsExhaustive] ?: base.strongsExhaustive,
                    nasExhaustiveOrigin = translation[GreekLexiconTranslations.nasExhaustiveOrigin] ?: base.nasExhaustiveOrigin,
                    nasExhaustiveDefinition = translation[GreekLexiconTranslations.nasExhaustiveDefinition] ?: base.nasExhaustiveDefinition,
                    nasExhaustiveTranslation = translation[GreekLexiconTranslations.nasExhaustiveTranslation] ?: base.nasExhaustiveTranslation
                )
            }
        }

        return base
    }

    private fun findHebrewWithTranslation(sn: String, locale: String): LexiconEntryDTO? {
        val entry = HebrewLexicon.selectAll()
            .where { HebrewLexicon.strongsNumber eq sn }
            .firstOrNull() ?: return null

        val base = entry.toHebrewDTO()

        if (locale != "en") {
            val translation = HebrewLexiconTranslations.selectAll()
                .where { (HebrewLexiconTranslations.lexiconId eq base.id) and (HebrewLexiconTranslations.locale eq locale) }
                .firstOrNull()

            if (translation != null) {
                return base.copy(
                    shortDefinition = translation[HebrewLexiconTranslations.shortDefinition] ?: base.shortDefinition,
                    fullDefinition = translation[HebrewLexiconTranslations.fullDefinition] ?: base.fullDefinition,
                    kjvTranslation = translation[HebrewLexiconTranslations.kjvTranslation] ?: base.kjvTranslation,
                    wordOrigin = translation[HebrewLexiconTranslations.wordOrigin] ?: base.wordOrigin,
                    strongsExhaustive = translation[HebrewLexiconTranslations.strongsExhaustive] ?: base.strongsExhaustive,
                    nasExhaustiveOrigin = translation[HebrewLexiconTranslations.nasExhaustiveOrigin] ?: base.nasExhaustiveOrigin,
                    nasExhaustiveDefinition = translation[HebrewLexiconTranslations.nasExhaustiveDefinition] ?: base.nasExhaustiveDefinition,
                    nasExhaustiveTranslation = translation[HebrewLexiconTranslations.nasExhaustiveTranslation] ?: base.nasExhaustiveTranslation
                )
            }
        }

        return base
    }
}
