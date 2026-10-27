/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.text

fun parseDomainFromEmail(email: String): String {
    val pattern = "[^.@]*?\\.\\w{2,}$|[^.@]*?\\.com?\\.\\w{2}$".toRegex()
    return pattern.find(email)?.value ?: throw IllegalArgumentException("$email does not have a valid domain")
}

/**
 * Normalize a domain string to a bare hostname (no protocol, no www, no trailing slash).
 * E.g. "https://www.liftview.nl/" → "liftview.nl"
 */
fun normalizeDomain(domain: String?): String? {
    if (domain.isNullOrBlank()) return null
    return domain.trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')
        .ifBlank { null }
}
