package com.github.cvzi.screenshottile.utils.ui

import java.util.Locale

internal const val FALLBACK_MARKER = "\u200c"

internal fun resolveLocalizedString(
    preferredLocales: List<Locale>,
    supportedLocales: List<Locale>,
    lookup: (Locale) -> String
): String? {
    val orderedPreferredLocales = normalizeRequestedLocales(preferredLocales, supportedLocales)
    for (candidate in orderedPreferredLocales) {
        val raw = lookup(candidate)
        if (raw.startsWith(FALLBACK_MARKER) && candidate.language != "en") {
            continue
        }
        val value = raw.removePrefix(FALLBACK_MARKER)
        return value
    }
    return null
}

internal fun normalizeRequestedLocales(
    preferredLocales: List<Locale>,
    supportedLocales: List<Locale>
): List<Locale> {
    val normalized = mutableListOf<Locale>()
    for (preferredLocale in preferredLocales) {
        val candidates = getCandidateLocales(preferredLocale, supportedLocales)
        for (candidate in candidates) {
            if (normalized.none { it.matchKey() == candidate.matchKey() }) {
                normalized.add(candidate)
            }
        }
    }
    return normalized
}

internal fun localeDedupeKey(locale: Locale): String {
    return locale.matchKey()
}

private fun Locale.isSpecific(): Boolean {
    return script.isNotBlank() || country.isNotBlank() || variant.isNotBlank()
}

private fun getCandidateLocales(
    preferredLocale: Locale,
    supportedLocales: List<Locale>
): List<Locale> {
    val sameLanguage = supportedLocales.filter { it.matchesLanguage(preferredLocale) }
    if (sameLanguage.isEmpty()) {
        return emptyList()
    }

    val exactMatch = sameLanguage.filter { it.matchKey() == preferredLocale.matchKey() }
    val genericLocales = sameLanguage.filter { !it.isSpecific() }
    val specificLocales = sameLanguage.filter { it.isSpecific() && it.matchKey() != preferredLocale.matchKey() }

    return if (preferredLocale.isSpecific()) {
        exactMatch + genericLocales + specificLocales
    } else {
        val orderedSpecifics = sameLanguage.filter { it.isSpecific() }
        orderedSpecifics + genericLocales
    }
}

private fun Locale.matchesLanguage(other: Locale): Boolean {
    return normalizedLanguage() == other.normalizedLanguage()
}

private fun Locale.matchKey(): String {
    return "${normalizedLanguage()}|${script}|${country}|${variant}"
}

private fun Locale.normalizedLanguage(): String {
    return when (language) {
        "in" -> "id"
        "iw" -> "he"
        else -> language
    }
}
