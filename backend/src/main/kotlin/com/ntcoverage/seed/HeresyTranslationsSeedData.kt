package com.ntcoverage.seed

data class HeresyTranslationSeedEntry(
    val normalizedName: String,
    val locale: String,
    val name: String,
    val description: String? = null
)

object HeresyTranslationsSeedData {
    val entries: List<HeresyTranslationSeedEntry> = listOf(
        HeresyTranslationSeedEntry("arianism", "pt", "Arianismo", "Doutrina que negava a plena coeternidade do Filho com o Pai."),
        HeresyTranslationSeedEntry("arianism", "es", "Arrianismo", "Doctrina que negaba la plena coeternidad del Hijo con el Padre."),
        HeresyTranslationSeedEntry("nestorianism", "pt", "Nestorianismo", "Corrente cristológica que separava excessivamente as naturezas em Cristo."),
        HeresyTranslationSeedEntry("nestorianism", "es", "Nestorianismo", "Corriente cristológica que separaba excesivamente las naturalezas de Cristo."),
        HeresyTranslationSeedEntry("monophysitism", "pt", "Monofisismo", "Doutrina que afirmava uma única natureza em Cristo após a encarnação."),
        HeresyTranslationSeedEntry("monophysitism", "es", "Monofisismo", "Doctrina que afirmaba una sola naturaleza en Cristo tras la encarnación."),
        HeresyTranslationSeedEntry("monothelitism", "pt", "Monotelismo", "Doutrina que atribuía uma única vontade a Cristo."),
        HeresyTranslationSeedEntry("monothelitism", "es", "Monotelismo", "Doctrina que atribuía una sola voluntad a Cristo."),
        HeresyTranslationSeedEntry("iconoclasm", "pt", "Iconoclasmo", "Movimento contrário ao uso e veneração de ícones."),
        HeresyTranslationSeedEntry("iconoclasm", "es", "Iconoclasia", "Movimiento contrario al uso y veneración de iconos."),
        HeresyTranslationSeedEntry("pelagianism", "pt", "Pelagianismo", "Negação da necessidade absoluta da graça para a salvação."),
        HeresyTranslationSeedEntry("pelagianism", "es", "Pelagianismo", "Negación de la necesidad absoluta de la gracia para la salvación."),
        HeresyTranslationSeedEntry("semi-pelagianism", "pt", "Semipelagianismo", "Postura intermediária na controvérsia da graça."),
        HeresyTranslationSeedEntry("semi-pelagianism", "es", "Semipelagianismo", "Postura intermedia en la controversia de la gracia."),
        HeresyTranslationSeedEntry("donatism", "pt", "Donatismo", "Cisma norte-africano sobre pureza da Igreja e validade sacramental."),
        HeresyTranslationSeedEntry("donatism", "es", "Donatismo", "Cisma norteafricano sobre pureza de la Iglesia y validez sacramental."),
        HeresyTranslationSeedEntry("novatianism", "pt", "Novacianismo", "Rigorismo disciplinar após perseguições."),
        HeresyTranslationSeedEntry("novatianism", "es", "Novacianismo", "Rigorismo disciplinario tras las persecuciones."),
        HeresyTranslationSeedEntry("gnosticism", "pt", "Gnosticismo", "Conjunto de correntes dualistas e esotéricas dos primeiros séculos."),
        HeresyTranslationSeedEntry("gnosticism", "es", "Gnosticismo", "Conjunto de corrientes dualistas y esotéricas de los primeros siglos."),
        HeresyTranslationSeedEntry("marcionism", "pt", "Marcionismo"),
        HeresyTranslationSeedEntry("marcionism", "es", "Marcionismo"),
        HeresyTranslationSeedEntry("montanism", "pt", "Montanismo"),
        HeresyTranslationSeedEntry("montanism", "es", "Montanismo"),
        HeresyTranslationSeedEntry("macedonianism", "pt", "Macedonianismo"),
        HeresyTranslationSeedEntry("macedonianism", "es", "Macedonianismo"),
        HeresyTranslationSeedEntry("apollinarianism", "pt", "Apolinarianismo"),
        HeresyTranslationSeedEntry("apollinarianism", "es", "Apolinarismo"),
        HeresyTranslationSeedEntry("adoptionism", "pt", "Adocionismo"),
        HeresyTranslationSeedEntry("adoptionism", "es", "Adopcionismo"),
        HeresyTranslationSeedEntry("paulicianism", "pt", "Paulicianismo"),
        HeresyTranslationSeedEntry("paulicianism", "es", "Paulicianismo"),
        HeresyTranslationSeedEntry("bogomilism", "pt", "Bogomilismo"),
        HeresyTranslationSeedEntry("bogomilism", "es", "Bogomilismo"),
        HeresyTranslationSeedEntry("photianism", "pt", "Fotianismo"),
        HeresyTranslationSeedEntry("photianism", "es", "Focianismo")
    )
}
