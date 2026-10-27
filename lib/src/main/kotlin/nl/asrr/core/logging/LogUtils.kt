/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.logging

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * De-duplicates recurring log lines so a persistent failure can't spam the log on a tight schedule.
 *
 * Tracks the last time each [key] was allowed through; identical messages within [windowMillis] are
 * suppressed. Use a key that captures the distinct identity of the problem (e.g. tenant + user + error)
 * so genuinely different failures still surface.
 *
 * Thread-safe; safe to share across scheduled and request-driven callers.
 */
class LogThrottle(
    private val windowMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val lastLogged = ConcurrentHashMap<String, Long>()

    /** Returns true the first time a [key] is seen and again only once [windowMillis] has elapsed. */
    fun shouldLog(key: String): Boolean {
        val now = clock()
        val previous = lastLogged.put(key, now)
        if (previous != null && now - previous < windowMillis) {
            // Within the window: keep the original timestamp so the window doesn't keep sliding forward.
            lastLogged[key] = previous
            return false
        }
        return true
    }
}

/**
 * An e-mail address fit for a log line: `jane@example.com` → `j**e@example.com`. Usernames are
 * e-mail addresses, and auth logs end up in places (Loki, Sentry) that must not hold PII.
 */
fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return "***"
    val local = parts[0]
    val masked = if (local.length <= 2) {
        "*".repeat(local.length)
    } else {
        "${local.first()}${"*".repeat(local.length - 2)}${local.last()}"
    }
    return "$masked@${parts[1]}"
}

/**
 * True when [exception] (or one of its causes) is the peer going away: a closed tab, a dropped
 * connection or a reset socket. These are expected on a WebSocket or a streamed download and not
 * worth a WARN, let alone a 500.
 */
fun isClientDisconnect(exception: Throwable): Boolean = generateSequence(exception) { it.cause.takeIf { cause -> cause !== it } }
    .take(10)
    .any { it is IOException }
