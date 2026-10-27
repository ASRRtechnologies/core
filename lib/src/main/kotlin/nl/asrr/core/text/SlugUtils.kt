/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.text

import java.text.Normalizer

private val DIACRITICS = "\\p{InCombiningDiacriticalMarks}+".toRegex()
private val NON_SLUG_CHARS = "[^a-z0-9]+".toRegex()

/**
 * URL-safe, lowercase slug: "Bouwbedrijf Dröse" → "bouwbedrijf-drose".
 *
 * Decomposes accented characters first so they degrade to their base letter instead of being
 * dropped, which would turn "Dröse" into "drse".
 *
 * Returns an empty string when nothing usable is left; callers decide on a fallback.
 */
fun slugify(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()
        .replace(NON_SLUG_CHARS, "-")
        .trim('-')
}
