package com.ntcoverage.seed

data class CouncilHereticParticipantSeedEntry(
    val councilSlug: String,
    val displayName: String,
    val role: String? = null,
    val description: String? = null
) {
    val normalizedName: String
        get() = displayName.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}

object CouncilHereticParticipantsSeedData {

    val entries: List<CouncilHereticParticipantSeedEntry> = listOf(
        // First Council of Nicaea (325)
        CouncilHereticParticipantSeedEntry(
            councilSlug = "nicaea-325",
            displayName = "Arius",
            role = "Presbyter of Alexandria",
            description = "Founder of Arianism; taught that the Son was created by the Father and not co-eternal."
        ),
        CouncilHereticParticipantSeedEntry(
            councilSlug = "nicaea-325",
            displayName = "Secundus of Ptolemais",
            role = "Bishop",
            description = "Arian bishop who refused to sign the Nicene Creed; exiled with Arius."
        ),
        CouncilHereticParticipantSeedEntry(
            councilSlug = "nicaea-325",
            displayName = "Theonas of Marmarike",
            role = "Bishop",
            description = "Arian bishop who refused to sign the Nicene Creed; exiled with Arius."
        ),

        // Council of Ephesus (431)
        CouncilHereticParticipantSeedEntry(
            councilSlug = "ephesus-431",
            displayName = "Nestorius",
            role = "Patriarch of Constantinople",
            description = "Taught that Christ had two distinct persons (divine and human); rejected the title Theotokos for Mary."
        ),

        // Council of Chalcedon (451)
        CouncilHereticParticipantSeedEntry(
            councilSlug = "chalcedon-451",
            displayName = "Eutyches",
            role = "Archimandrite of Constantinople",
            description = "Taught Monophysitism; held that Christ had only one nature after the Incarnation."
        ),
        CouncilHereticParticipantSeedEntry(
            councilSlug = "chalcedon-451",
            displayName = "Dioscorus of Alexandria",
            role = "Patriarch of Alexandria",
            description = "Presided at the Robber Council of Ephesus (449); deposed at Chalcedon for supporting Eutyches."
        ),

        // Second Council of Constantinople (553) - Three Chapters
        CouncilHereticParticipantSeedEntry(
            councilSlug = "constantinople-553",
            displayName = "Theodore of Mopsuestia",
            role = "Bishop (posthumously condemned)",
            description = "His writings were condemned in the Three Chapters controversy as Nestorian."
        ),
        CouncilHereticParticipantSeedEntry(
            councilSlug = "constantinople-553",
            displayName = "Ibas of Edessa",
            role = "Bishop (posthumously condemned)",
            description = "His letter to Mari was condemned in the Three Chapters controversy."
        ),

        // Third Council of Constantinople (680)
        CouncilHereticParticipantSeedEntry(
            councilSlug = "constantinople-680",
            displayName = "Honorius I",
            role = "Pope of Rome (posthumously condemned)",
            description = "Condemned for Monothelite teaching in his letters to Sergius."
        ),
        CouncilHereticParticipantSeedEntry(
            councilSlug = "constantinople-680",
            displayName = "Sergius I",
            role = "Patriarch of Constantinople",
            description = "Promoted Monoenergism and Monothelitism; condemned posthumously."
        ),

        // Second Council of Nicaea (787) - Iconoclasm
        CouncilHereticParticipantSeedEntry(
            councilSlug = "nicaea-787",
            displayName = "Constantine V",
            role = "Emperor",
            description = "Iconoclast emperor who convened the Council of Hieria (754) against icons."
        ),

        // Council of Orange II (529) - Semi-Pelagianism
        CouncilHereticParticipantSeedEntry(
            councilSlug = "orange-529",
            displayName = "Semi-Pelagians",
            role = "Monks of Southern Gaul",
            description = "Taught that human free will could initiate faith without prevenient grace."
        ),

        // Councils of Arabia (246-247) - Thnetopsychism
        CouncilHereticParticipantSeedEntry(
            councilSlug = "councils-of-arabia-246",
            displayName = "Beryllus of Bostra",
            role = "Bishop of Bostra",
            description = "Taught that the soul dies with the body (thnetopsychism); recanted after Origen's refutation at the councils."
        )
    )
}
