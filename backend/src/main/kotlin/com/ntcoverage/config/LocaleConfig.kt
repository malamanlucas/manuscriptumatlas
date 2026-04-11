package com.ntcoverage.config

object LocaleConfig {
    val allowedLocales = setOf("en", "pt", "es")

    fun sanitize(raw: String?): String =
        raw?.takeIf { it in allowedLocales } ?: "en"
}
