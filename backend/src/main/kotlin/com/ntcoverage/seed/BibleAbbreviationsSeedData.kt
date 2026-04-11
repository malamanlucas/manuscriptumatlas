package com.ntcoverage.seed

object BibleAbbreviationsSeedData {
    data class AbbreviationEntry(
        val bookName: String,
        val locale: String,
        val abbreviations: List<String>
    )

    val entries = listOf(
        // GENESIS
        AbbreviationEntry("Genesis", "en", listOf("Gen", "Ge", "Gn")),
        AbbreviationEntry("Genesis", "pt", listOf("Gn", "Gên", "Gen")),
        AbbreviationEntry("Genesis", "es", listOf("Gn", "Gén", "Gen")),
        // EXODUS
        AbbreviationEntry("Exodus", "en", listOf("Exod", "Ex")),
        AbbreviationEntry("Exodus", "pt", listOf("Êx", "Ex")),
        AbbreviationEntry("Exodus", "es", listOf("Éx", "Ex")),
        // LEVITICUS
        AbbreviationEntry("Leviticus", "en", listOf("Lev", "Lv")),
        AbbreviationEntry("Leviticus", "pt", listOf("Lv", "Lev")),
        AbbreviationEntry("Leviticus", "es", listOf("Lv", "Lev")),
        // NUMBERS
        AbbreviationEntry("Numbers", "en", listOf("Num", "Nu", "Nm")),
        AbbreviationEntry("Numbers", "pt", listOf("Nm", "Núm")),
        AbbreviationEntry("Numbers", "es", listOf("Nm", "Núm")),
        // DEUTERONOMY
        AbbreviationEntry("Deuteronomy", "en", listOf("Deut", "Dt")),
        AbbreviationEntry("Deuteronomy", "pt", listOf("Dt", "Deut")),
        AbbreviationEntry("Deuteronomy", "es", listOf("Dt", "Deut")),
        // JOSHUA
        AbbreviationEntry("Joshua", "en", listOf("Josh", "Jos")),
        AbbreviationEntry("Joshua", "pt", listOf("Js", "Jos")),
        AbbreviationEntry("Joshua", "es", listOf("Jos")),
        // JUDGES
        AbbreviationEntry("Judges", "en", listOf("Judg", "Jdg")),
        AbbreviationEntry("Judges", "pt", listOf("Jz")),
        AbbreviationEntry("Judges", "es", listOf("Jue")),
        // RUTH
        AbbreviationEntry("Ruth", "en", listOf("Ruth", "Ru")),
        AbbreviationEntry("Ruth", "pt", listOf("Rt")),
        AbbreviationEntry("Ruth", "es", listOf("Rut", "Rt")),
        // 1 SAMUEL
        AbbreviationEntry("1 Samuel", "en", listOf("1Sam", "1Sa", "1 Sam")),
        AbbreviationEntry("1 Samuel", "pt", listOf("1Sm", "1 Sm", "1 Samuel")),
        AbbreviationEntry("1 Samuel", "es", listOf("1Sam", "1 Sam", "1 Samuel")),
        // 2 SAMUEL
        AbbreviationEntry("2 Samuel", "en", listOf("2Sam", "2Sa", "2 Sam")),
        AbbreviationEntry("2 Samuel", "pt", listOf("2Sm", "2 Sm", "2 Samuel")),
        AbbreviationEntry("2 Samuel", "es", listOf("2Sam", "2 Sam", "2 Samuel")),
        // 1 KINGS
        AbbreviationEntry("1 Kings", "en", listOf("1Kgs", "1Ki", "1 Kings")),
        AbbreviationEntry("1 Kings", "pt", listOf("1Rs", "1 Rs", "1 Reis")),
        AbbreviationEntry("1 Kings", "es", listOf("1Re", "1 Re", "1 Reyes")),
        // 2 KINGS
        AbbreviationEntry("2 Kings", "en", listOf("2Kgs", "2Ki", "2 Kings")),
        AbbreviationEntry("2 Kings", "pt", listOf("2Rs", "2 Rs", "2 Reis")),
        AbbreviationEntry("2 Kings", "es", listOf("2Re", "2 Re", "2 Reyes")),
        // 1 CHRONICLES
        AbbreviationEntry("1 Chronicles", "en", listOf("1Chr", "1Ch", "1 Chr")),
        AbbreviationEntry("1 Chronicles", "pt", listOf("1Cr", "1 Cr", "1 Crônicas")),
        AbbreviationEntry("1 Chronicles", "es", listOf("1Cr", "1 Cr", "1 Crónicas")),
        // 2 CHRONICLES
        AbbreviationEntry("2 Chronicles", "en", listOf("2Chr", "2Ch", "2 Chr")),
        AbbreviationEntry("2 Chronicles", "pt", listOf("2Cr", "2 Cr", "2 Crônicas")),
        AbbreviationEntry("2 Chronicles", "es", listOf("2Cr", "2 Cr", "2 Crónicas")),
        // EZRA
        AbbreviationEntry("Ezra", "en", listOf("Ezra", "Ezr")),
        AbbreviationEntry("Ezra", "pt", listOf("Ed", "Esd")),
        AbbreviationEntry("Ezra", "es", listOf("Esd")),
        // NEHEMIAH
        AbbreviationEntry("Nehemiah", "en", listOf("Neh", "Ne")),
        AbbreviationEntry("Nehemiah", "pt", listOf("Ne")),
        AbbreviationEntry("Nehemiah", "es", listOf("Neh", "Ne")),
        // ESTHER
        AbbreviationEntry("Esther", "en", listOf("Esth", "Est")),
        AbbreviationEntry("Esther", "pt", listOf("Et", "Est")),
        AbbreviationEntry("Esther", "es", listOf("Est")),
        // JOB
        AbbreviationEntry("Job", "en", listOf("Job")),
        AbbreviationEntry("Job", "pt", listOf("Jó")),
        AbbreviationEntry("Job", "es", listOf("Job")),
        // PSALMS
        AbbreviationEntry("Psalms", "en", listOf("Ps", "Psa")),
        AbbreviationEntry("Psalms", "pt", listOf("Sl", "Salmo", "Salmos")),
        AbbreviationEntry("Psalms", "es", listOf("Sal")),
        // PROVERBS
        AbbreviationEntry("Proverbs", "en", listOf("Prov", "Pr")),
        AbbreviationEntry("Proverbs", "pt", listOf("Pv", "Prov")),
        AbbreviationEntry("Proverbs", "es", listOf("Pr", "Prov")),
        // ECCLESIASTES
        AbbreviationEntry("Ecclesiastes", "en", listOf("Eccl", "Ecc")),
        AbbreviationEntry("Ecclesiastes", "pt", listOf("Ec", "Ecl")),
        AbbreviationEntry("Ecclesiastes", "es", listOf("Ec", "Ecl")),
        // SONG OF SOLOMON
        AbbreviationEntry("Song of Solomon", "en", listOf("Song", "SoS", "SS")),
        AbbreviationEntry("Song of Solomon", "pt", listOf("Ct", "Cantares")),
        AbbreviationEntry("Song of Solomon", "es", listOf("Cnt", "Cantares")),
        // ISAIAH
        AbbreviationEntry("Isaiah", "en", listOf("Isa", "Is")),
        AbbreviationEntry("Isaiah", "pt", listOf("Is")),
        AbbreviationEntry("Isaiah", "es", listOf("Is")),
        // JEREMIAH
        AbbreviationEntry("Jeremiah", "en", listOf("Jer", "Je")),
        AbbreviationEntry("Jeremiah", "pt", listOf("Jr", "Jer")),
        AbbreviationEntry("Jeremiah", "es", listOf("Jer", "Jr")),
        // LAMENTATIONS
        AbbreviationEntry("Lamentations", "en", listOf("Lam", "La")),
        AbbreviationEntry("Lamentations", "pt", listOf("Lm")),
        AbbreviationEntry("Lamentations", "es", listOf("Lm", "Lam")),
        // EZEKIEL
        AbbreviationEntry("Ezekiel", "en", listOf("Ezek", "Eze")),
        AbbreviationEntry("Ezekiel", "pt", listOf("Ez")),
        AbbreviationEntry("Ezekiel", "es", listOf("Ez")),
        // DANIEL
        AbbreviationEntry("Daniel", "en", listOf("Dan", "Da")),
        AbbreviationEntry("Daniel", "pt", listOf("Dn", "Dan")),
        AbbreviationEntry("Daniel", "es", listOf("Dn", "Dan")),
        // HOSEA
        AbbreviationEntry("Hosea", "en", listOf("Hos", "Ho")),
        AbbreviationEntry("Hosea", "pt", listOf("Os")),
        AbbreviationEntry("Hosea", "es", listOf("Os")),
        // JOEL
        AbbreviationEntry("Joel", "en", listOf("Joel", "Jl")),
        AbbreviationEntry("Joel", "pt", listOf("Jl")),
        AbbreviationEntry("Joel", "es", listOf("Jl")),
        // AMOS
        AbbreviationEntry("Amos", "en", listOf("Amos", "Am")),
        AbbreviationEntry("Amos", "pt", listOf("Am")),
        AbbreviationEntry("Amos", "es", listOf("Am")),
        // OBADIAH
        AbbreviationEntry("Obadiah", "en", listOf("Obad", "Ob")),
        AbbreviationEntry("Obadiah", "pt", listOf("Ob")),
        AbbreviationEntry("Obadiah", "es", listOf("Abd", "Ob")),
        // JONAH
        AbbreviationEntry("Jonah", "en", listOf("Jonah", "Jon")),
        AbbreviationEntry("Jonah", "pt", listOf("Jn", "Jonas")),
        AbbreviationEntry("Jonah", "es", listOf("Jon")),
        // MICAH
        AbbreviationEntry("Micah", "en", listOf("Mic", "Mi")),
        AbbreviationEntry("Micah", "pt", listOf("Mq")),
        AbbreviationEntry("Micah", "es", listOf("Mi", "Miq")),
        // NAHUM
        AbbreviationEntry("Nahum", "en", listOf("Nah", "Na")),
        AbbreviationEntry("Nahum", "pt", listOf("Na")),
        AbbreviationEntry("Nahum", "es", listOf("Nah", "Na")),
        // HABAKKUK
        AbbreviationEntry("Habakkuk", "en", listOf("Hab")),
        AbbreviationEntry("Habakkuk", "pt", listOf("Hc", "Hab")),
        AbbreviationEntry("Habakkuk", "es", listOf("Hab")),
        // ZEPHANIAH
        AbbreviationEntry("Zephaniah", "en", listOf("Zeph", "Zep")),
        AbbreviationEntry("Zephaniah", "pt", listOf("Sf")),
        AbbreviationEntry("Zephaniah", "es", listOf("Sof")),
        // HAGGAI
        AbbreviationEntry("Haggai", "en", listOf("Hag")),
        AbbreviationEntry("Haggai", "pt", listOf("Ag")),
        AbbreviationEntry("Haggai", "es", listOf("Hag")),
        // ZECHARIAH
        AbbreviationEntry("Zechariah", "en", listOf("Zech", "Zec")),
        AbbreviationEntry("Zechariah", "pt", listOf("Zc")),
        AbbreviationEntry("Zechariah", "es", listOf("Zac")),
        // MALACHI
        AbbreviationEntry("Malachi", "en", listOf("Mal")),
        AbbreviationEntry("Malachi", "pt", listOf("Ml")),
        AbbreviationEntry("Malachi", "es", listOf("Mal")),

        // NEW TESTAMENT
        // MATTHEW
        AbbreviationEntry("Matthew", "en", listOf("Matt", "Mt")),
        AbbreviationEntry("Matthew", "pt", listOf("Mt")),
        AbbreviationEntry("Matthew", "es", listOf("Mt")),
        // MARK
        AbbreviationEntry("Mark", "en", listOf("Mark", "Mk")),
        AbbreviationEntry("Mark", "pt", listOf("Mc")),
        AbbreviationEntry("Mark", "es", listOf("Mc")),
        // LUKE
        AbbreviationEntry("Luke", "en", listOf("Luke", "Lk")),
        AbbreviationEntry("Luke", "pt", listOf("Lc")),
        AbbreviationEntry("Luke", "es", listOf("Lc")),
        // JOHN
        AbbreviationEntry("John", "en", listOf("John", "Jn")),
        AbbreviationEntry("John", "pt", listOf("Jo")),
        AbbreviationEntry("John", "es", listOf("Jn")),
        // ACTS
        AbbreviationEntry("Acts", "en", listOf("Acts", "Act")),
        AbbreviationEntry("Acts", "pt", listOf("At", "Atos")),
        AbbreviationEntry("Acts", "es", listOf("Hch", "Hechos")),
        // ROMANS
        AbbreviationEntry("Romans", "en", listOf("Rom", "Ro")),
        AbbreviationEntry("Romans", "pt", listOf("Rm")),
        AbbreviationEntry("Romans", "es", listOf("Ro", "Rom")),
        // 1 CORINTHIANS
        AbbreviationEntry("1 Corinthians", "en", listOf("1Cor", "1Co", "1 Cor")),
        AbbreviationEntry("1 Corinthians", "pt", listOf("1Co", "1 Co")),
        AbbreviationEntry("1 Corinthians", "es", listOf("1Co", "1 Co")),
        // 2 CORINTHIANS
        AbbreviationEntry("2 Corinthians", "en", listOf("2Cor", "2Co", "2 Cor")),
        AbbreviationEntry("2 Corinthians", "pt", listOf("2Co", "2 Co")),
        AbbreviationEntry("2 Corinthians", "es", listOf("2Co", "2 Co")),
        // GALATIANS
        AbbreviationEntry("Galatians", "en", listOf("Gal", "Ga")),
        AbbreviationEntry("Galatians", "pt", listOf("Gl", "Gal")),
        AbbreviationEntry("Galatians", "es", listOf("Gá", "Gal")),
        // EPHESIANS
        AbbreviationEntry("Ephesians", "en", listOf("Eph")),
        AbbreviationEntry("Ephesians", "pt", listOf("Ef")),
        AbbreviationEntry("Ephesians", "es", listOf("Ef")),
        // PHILIPPIANS
        AbbreviationEntry("Philippians", "en", listOf("Phil", "Php")),
        AbbreviationEntry("Philippians", "pt", listOf("Fp", "Fil")),
        AbbreviationEntry("Philippians", "es", listOf("Fil")),
        // COLOSSIANS
        AbbreviationEntry("Colossians", "en", listOf("Col")),
        AbbreviationEntry("Colossians", "pt", listOf("Cl", "Col")),
        AbbreviationEntry("Colossians", "es", listOf("Col")),
        // 1 THESSALONIANS
        AbbreviationEntry("1 Thessalonians", "en", listOf("1Thess", "1Th", "1 Thess")),
        AbbreviationEntry("1 Thessalonians", "pt", listOf("1Ts", "1 Ts")),
        AbbreviationEntry("1 Thessalonians", "es", listOf("1Ts", "1 Ts")),
        // 2 THESSALONIANS
        AbbreviationEntry("2 Thessalonians", "en", listOf("2Thess", "2Th", "2 Thess")),
        AbbreviationEntry("2 Thessalonians", "pt", listOf("2Ts", "2 Ts")),
        AbbreviationEntry("2 Thessalonians", "es", listOf("2Ts", "2 Ts")),
        // 1 TIMOTHY
        AbbreviationEntry("1 Timothy", "en", listOf("1Tim", "1Ti", "1 Tim")),
        AbbreviationEntry("1 Timothy", "pt", listOf("1Tm", "1 Tm")),
        AbbreviationEntry("1 Timothy", "es", listOf("1Ti", "1 Ti")),
        // 2 TIMOTHY
        AbbreviationEntry("2 Timothy", "en", listOf("2Tim", "2Ti", "2 Tim")),
        AbbreviationEntry("2 Timothy", "pt", listOf("2Tm", "2 Tm")),
        AbbreviationEntry("2 Timothy", "es", listOf("2Ti", "2 Ti")),
        // TITUS
        AbbreviationEntry("Titus", "en", listOf("Titus", "Tit")),
        AbbreviationEntry("Titus", "pt", listOf("Tt")),
        AbbreviationEntry("Titus", "es", listOf("Tit")),
        // PHILEMON
        AbbreviationEntry("Philemon", "en", listOf("Phlm", "Phm")),
        AbbreviationEntry("Philemon", "pt", listOf("Fm")),
        AbbreviationEntry("Philemon", "es", listOf("Flm")),
        // HEBREWS
        AbbreviationEntry("Hebrews", "en", listOf("Heb")),
        AbbreviationEntry("Hebrews", "pt", listOf("Hb")),
        AbbreviationEntry("Hebrews", "es", listOf("He", "Heb")),
        // JAMES
        AbbreviationEntry("James", "en", listOf("Jas")),
        AbbreviationEntry("James", "pt", listOf("Tg")),
        AbbreviationEntry("James", "es", listOf("Stg")),
        // 1 PETER
        AbbreviationEntry("1 Peter", "en", listOf("1Pet", "1Pe", "1 Pet")),
        AbbreviationEntry("1 Peter", "pt", listOf("1Pe", "1 Pe")),
        AbbreviationEntry("1 Peter", "es", listOf("1Pe", "1 Pe")),
        // 2 PETER
        AbbreviationEntry("2 Peter", "en", listOf("2Pet", "2Pe", "2 Pet")),
        AbbreviationEntry("2 Peter", "pt", listOf("2Pe", "2 Pe")),
        AbbreviationEntry("2 Peter", "es", listOf("2Pe", "2 Pe")),
        // 1 JOHN
        AbbreviationEntry("1 John", "en", listOf("1John", "1Jn", "1 John")),
        AbbreviationEntry("1 John", "pt", listOf("1Jo", "1 Jo")),
        AbbreviationEntry("1 John", "es", listOf("1Jn", "1 Jn")),
        // 2 JOHN
        AbbreviationEntry("2 John", "en", listOf("2John", "2Jn", "2 John")),
        AbbreviationEntry("2 John", "pt", listOf("2Jo", "2 Jo")),
        AbbreviationEntry("2 John", "es", listOf("2Jn", "2 Jn")),
        // 3 JOHN
        AbbreviationEntry("3 John", "en", listOf("3John", "3Jn", "3 John")),
        AbbreviationEntry("3 John", "pt", listOf("3Jo", "3 Jo")),
        AbbreviationEntry("3 John", "es", listOf("3Jn", "3 Jn")),
        // JUDE
        AbbreviationEntry("Jude", "en", listOf("Jude")),
        AbbreviationEntry("Jude", "pt", listOf("Jd")),
        AbbreviationEntry("Jude", "es", listOf("Jud")),
        // REVELATION
        AbbreviationEntry("Revelation", "en", listOf("Rev", "Re")),
        AbbreviationEntry("Revelation", "pt", listOf("Ap")),
        AbbreviationEntry("Revelation", "es", listOf("Ap"))
    )
}
