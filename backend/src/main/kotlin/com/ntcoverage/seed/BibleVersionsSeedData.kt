package com.ntcoverage.seed

object BibleVersionsSeedData {
    data class VersionEntry(
        val code: String,
        val name: String,
        val language: String,
        val description: String,
        val isPrimary: Boolean,
        val testamentScope: String
    )

    val entries = listOf(
        VersionEntry("KJV", "King James Version", "en", "1611 English translation, public domain", true, "FULL"),
        VersionEntry("AA", "Almeida Atualizada", "pt", "Tradução portuguesa atualizada, domínio público", false, "FULL"),
        VersionEntry("ACF", "Almeida Corrigida Fiel", "pt", "Tradução portuguesa fiel ao Textus Receptus, domínio público", false, "FULL"),
        VersionEntry("ARC69", "Almeida Revista e Corrigida 1969", "pt", "Tradução portuguesa clássica de 1969", false, "FULL")
    )
}
