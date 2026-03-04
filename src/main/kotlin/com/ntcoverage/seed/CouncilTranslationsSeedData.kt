package com.ntcoverage.seed

data class CouncilTranslationSeedEntry(
    val councilSlug: String,
    val locale: String,
    val displayName: String,
    val shortDescription: String? = null,
    val location: String? = null,
    val mainTopics: String? = null,
    val summary: String? = null
)

object CouncilTranslationsSeedData {
    val entries: List<CouncilTranslationSeedEntry> = listOf(
        CouncilTranslationSeedEntry("nicaea-325", "pt", "Primeiro Concílio de Niceia", "Primeiro concílio ecumênico convocado por Constantino.", "Niceia", "Arianismo; Credo Niceno"),
        CouncilTranslationSeedEntry("nicaea-325", "es", "Primer Concilio de Nicea", "Primer concilio ecuménico convocado por Constantino.", "Nicea", "Arrianismo; Credo Niceno"),
        CouncilTranslationSeedEntry("constantinople-381", "pt", "Primeiro Concílio de Constantinopla", "Segundo concílio ecumênico.", "Constantinopla", "Macedonianismo; reafirmação nicena"),
        CouncilTranslationSeedEntry("constantinople-381", "es", "Primer Concilio de Constantinopla", "Segundo concilio ecuménico.", "Constantinopla", "Macedonianismo; reafirmación nicena"),
        CouncilTranslationSeedEntry("ephesus-431", "pt", "Concílio de Éfeso", "Terceiro concílio ecumênico.", "Éfeso", "Nestorianismo; Theotokos"),
        CouncilTranslationSeedEntry("ephesus-431", "es", "Concilio de Éfeso", "Tercer concilio ecuménico.", "Éfeso", "Nestorianismo; Theotokos"),
        CouncilTranslationSeedEntry("chalcedon-451", "pt", "Concílio de Calcedônia", "Quarto concílio ecumênico.", "Calcedônia", "Cristologia; monofisismo"),
        CouncilTranslationSeedEntry("chalcedon-451", "es", "Concilio de Calcedonia", "Cuarto concilio ecuménico.", "Calcedonia", "Cristología; monofisismo"),
        CouncilTranslationSeedEntry("constantinople-553", "pt", "Segundo Concílio de Constantinopla", "Quinto concílio ecumênico.", "Constantinopla", "Controvérsia dos Três Capítulos"),
        CouncilTranslationSeedEntry("constantinople-553", "es", "Segundo Concilio de Constantinopla", "Quinto concilio ecuménico.", "Constantinopla", "Controversia de los Tres Capítulos"),
        CouncilTranslationSeedEntry("constantinople-680", "pt", "Terceiro Concílio de Constantinopla", "Sexto concílio ecumênico.", "Constantinopla", "Monotelismo"),
        CouncilTranslationSeedEntry("constantinople-680", "es", "Tercer Concilio de Constantinopla", "Sexto concilio ecuménico.", "Constantinopla", "Monotelismo"),
        CouncilTranslationSeedEntry("nicaea-787", "pt", "Segundo Concílio de Niceia", "Sétimo concílio ecumênico.", "Niceia", "Iconoclasmo"),
        CouncilTranslationSeedEntry("nicaea-787", "es", "Segundo Concilio de Nicea", "Séptimo concilio ecuménico.", "Nicea", "Iconoclasia"),
        CouncilTranslationSeedEntry("constantinople-869", "pt", "Quarto Concílio de Constantinopla", "Concílio ecumênico na tradição católica.", "Constantinopla", "Cisma fotiano"),
        CouncilTranslationSeedEntry("constantinople-869", "es", "Cuarto Concilio de Constantinopla", "Concilio ecuménico en la tradición católica.", "Constantinopla", "Cisma fociano"),
        CouncilTranslationSeedEntry("elvira-306", "pt", "Sínodo de Elvira", location = "Elvira"),
        CouncilTranslationSeedEntry("elvira-306", "es", "Sínodo de Elvira", location = "Elvira"),
        CouncilTranslationSeedEntry("arles-314", "pt", "Sínodo de Arles (314)", location = "Arles"),
        CouncilTranslationSeedEntry("arles-314", "es", "Sínodo de Arlés (314)", location = "Arlés"),
        CouncilTranslationSeedEntry("ancyra-314", "pt", "Sínodo de Ancira", location = "Ancira"),
        CouncilTranslationSeedEntry("ancyra-314", "es", "Sínodo de Ancira", location = "Ancira"),
        CouncilTranslationSeedEntry("sardica-343", "pt", "Concílio de Sárdica", location = "Sárdica"),
        CouncilTranslationSeedEntry("sardica-343", "es", "Concilio de Sárdica", location = "Sárdica"),
        CouncilTranslationSeedEntry("laodicea-363", "pt", "Sínodo de Laodiceia", location = "Laodiceia"),
        CouncilTranslationSeedEntry("laodicea-363", "es", "Sínodo de Laodicea", location = "Laodicea"),
        CouncilTranslationSeedEntry("toledo-400", "pt", "Primeiro Concílio de Toledo", location = "Toledo"),
        CouncilTranslationSeedEntry("toledo-400", "es", "Primer Concilio de Toledo", location = "Toledo"),
        CouncilTranslationSeedEntry("toledo-589", "pt", "Terceiro Concílio de Toledo", location = "Toledo"),
        CouncilTranslationSeedEntry("toledo-589", "es", "Tercer Concilio de Toledo", location = "Toledo"),
        CouncilTranslationSeedEntry("orange-529", "pt", "Concílio de Orange II", location = "Orange", mainTopics = "Graça; semipelagianismo"),
        CouncilTranslationSeedEntry("orange-529", "es", "Concilio de Orange II", location = "Orange", mainTopics = "Gracia; semipelagianismo"),
        CouncilTranslationSeedEntry("whitby-664", "pt", "Sínodo de Whitby", location = "Whitby"),
        CouncilTranslationSeedEntry("whitby-664", "es", "Sínodo de Whitby", location = "Whitby"),
        CouncilTranslationSeedEntry("frankfurt-794", "pt", "Concílio de Frankfurt", location = "Frankfurt"),
        CouncilTranslationSeedEntry("frankfurt-794", "es", "Concilio de Frankfurt", location = "Frankfurt"),
        CouncilTranslationSeedEntry("quierzy-853", "pt", "Concílio de Quierzy", location = "Quierzy"),
        CouncilTranslationSeedEntry("quierzy-853", "es", "Concilio de Quierzy", location = "Quierzy"),
        CouncilTranslationSeedEntry("charroux-989", "pt", "Sínodo de Charroux", location = "Charroux"),
        CouncilTranslationSeedEntry("charroux-989", "es", "Sínodo de Charroux", location = "Charroux")
    )
}
