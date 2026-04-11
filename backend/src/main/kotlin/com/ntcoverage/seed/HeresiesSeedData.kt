package com.ntcoverage.seed

data class HeresySeedEntry(
    val name: String,
    val normalizedName: String,
    val slug: String,
    val description: String? = null,
    val centuryOrigin: Int? = null,
    val yearOrigin: Int? = null,
    val keyFigure: String? = null,
    val wikipediaUrl: String? = null
)

object HeresiesSeedData {
    val entries: List<HeresySeedEntry> = listOf(
        HeresySeedEntry("Gnosticism", "gnosticism", "gnosticism", "Early dualistic movements and esoteric cosmologies.", 2, 120, "Valentinus", "https://en.wikipedia.org/wiki/Gnosticism"),
        HeresySeedEntry("Marcionism", "marcionism", "marcionism", "Rejection of the Old Testament and its God.", 2, 140, "Marcion", "https://en.wikipedia.org/wiki/Marcionism"),
        HeresySeedEntry("Montanism", "montanism", "montanism", "Prophetic movement emphasizing ecstatic revelation.", 2, 156, "Montanus", "https://en.wikipedia.org/wiki/Montanism"),
        HeresySeedEntry("Monarchianism", "monarchianism", "monarchianism", "Modalist and dynamic strains stressing divine monarchy.", 2, 190, "Sabellius", "https://en.wikipedia.org/wiki/Monarchianism"),
        HeresySeedEntry("Novatianism", "novatianism", "novatianism", "Rigorous treatment of the lapsed after persecution.", 3, 251, "Novatian", "https://en.wikipedia.org/wiki/Novatianism"),
        HeresySeedEntry("Donatism", "donatism", "donatism", "North African schism concerning validity of sacraments.", 4, 311, "Donatus Magnus", "https://en.wikipedia.org/wiki/Donatism"),
        HeresySeedEntry("Arianism", "arianism", "arianism", "Christ is not co-eternal with the Father.", 4, 318, "Arius", "https://en.wikipedia.org/wiki/Arianism"),
        HeresySeedEntry("Macedonianism", "macedonianism", "macedonianism", "Denied full divinity of the Holy Spirit.", 4, 360, "Macedonius I", "https://en.wikipedia.org/wiki/Pneumatomachi"),
        HeresySeedEntry("Apollinarianism", "apollinarianism", "apollinarianism", "Denied full human rational soul in Christ.", 4, 360, "Apollinaris of Laodicea", "https://en.wikipedia.org/wiki/Apollinarism"),
        HeresySeedEntry("Pelagianism", "pelagianism", "pelagianism", "Denied original sin and necessity of grace as taught by Augustine.", 5, 405, "Pelagius", "https://en.wikipedia.org/wiki/Pelagianism"),
        HeresySeedEntry("Semi-Pelagianism", "semi-pelagianism", "semi-pelagianism", "Affirmed initial movement toward God without grace.", 5, 420, "John Cassian", "https://en.wikipedia.org/wiki/Semi-Pelagianism"),
        HeresySeedEntry("Nestorianism", "nestorianism", "nestorianism", "Separation of Christ's natures and persons.", 5, 428, "Nestorius", "https://en.wikipedia.org/wiki/Nestorianism"),
        HeresySeedEntry("Monophysitism", "monophysitism", "monophysitism", "Single-nature Christology after the incarnation.", 5, 448, "Eutyches", "https://en.wikipedia.org/wiki/Monophysitism"),
        HeresySeedEntry("Origenism", "origenism", "origenism", "Speculative doctrines attributed to Origenist circles.", 4, 370, "Evagrius Ponticus", "https://en.wikipedia.org/wiki/Origenism"),
        HeresySeedEntry("Three Chapters", "three-chapters", "three-chapters-controversy", "Controversy over Theodore, Theodoret and Ibas writings.", 6, 544, "Theodore of Mopsuestia", "https://en.wikipedia.org/wiki/Three-Chapter_Controversy"),
        HeresySeedEntry("Monothelitism", "monothelitism", "monothelitism", "Christ has one will.", 7, 633, "Sergius I of Constantinople", "https://en.wikipedia.org/wiki/Monothelitism"),
        HeresySeedEntry("Monoenergism", "monoenergism", "monoenergism", "Christ has one mode of operation.", 7, 620, "Sergius I of Constantinople", "https://en.wikipedia.org/wiki/Monoenergism"),
        HeresySeedEntry("Iconoclasm", "iconoclasm", "iconoclasm", "Rejection of sacred images and icons.", 8, 726, "Leo III", "https://en.wikipedia.org/wiki/Byzantine_Iconoclasm"),
        HeresySeedEntry("Adoptionism", "adoptionism", "adoptionism", "Christ as adopted Son in a particular sense.", 8, 780, "Elipandus of Toledo", "https://en.wikipedia.org/wiki/Adoptionism"),
        HeresySeedEntry("Paulicianism", "paulicianism", "paulicianism", "Dualist movement in Armenia and Byzantium.", 7, 650, "Constantine-Silvanus", "https://en.wikipedia.org/wiki/Paulicianism"),
        HeresySeedEntry("Bogomilism", "bogomilism", "bogomilism", "Dualist movement from the Balkans.", 10, 930, "Bogomil", "https://en.wikipedia.org/wiki/Bogomilism"),
        HeresySeedEntry("Docetism", "docetism", "docetism", "Christ only seemed to have a human body.", 2, 110, "Various gnostic teachers", "https://en.wikipedia.org/wiki/Docetism"),
        HeresySeedEntry("Ebionism", "ebionism", "ebionism", "Jewish-Christian movement with low Christology.", 2, 100, "Ebionites", "https://en.wikipedia.org/wiki/Ebionites"),
        HeresySeedEntry("Manichaeism", "manichaeism", "manichaeism", "Religious dualism founded by Mani.", 3, 240, "Mani", "https://en.wikipedia.org/wiki/Manichaeism"),
        HeresySeedEntry("Priscillianism", "priscillianism", "priscillianism", "Ascetic movement condemned in Hispania.", 4, 370, "Priscillian", "https://en.wikipedia.org/wiki/Priscillianism"),
        HeresySeedEntry("Audians", "audians", "audians", "Anthropomorphic tendencies and schismatic practices.", 4, 340, "Audius", "https://en.wikipedia.org/wiki/Audians"),
        HeresySeedEntry("Photianism", "photianism", "photianism", "Ecclesiological conflict around Photius and Roman primacy.", 9, 863, "Photius I", "https://en.wikipedia.org/wiki/Photian_schism")
    )
}
