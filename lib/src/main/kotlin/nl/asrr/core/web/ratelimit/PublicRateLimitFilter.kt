/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.web.ratelimit

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Per-IP rate limiting for abuse-sensitive public POST endpoints (lead forms, sign-up, magic links).
 *
 * Runs in one of three [Mode]s:
 *
 * - `OFF` — every request passes, nothing is counted.
 * - `SHADOW` — requests are counted and every one that *would* be blocked is logged as a WARN
 *   (`public rate limit exceeded (shadow)`) but still passes through. Start here: watch those lines
 *   per tenant/IP before enforcing, so a busy customer office behind one NAT never finds out about
 *   the limit by losing a lead.
 * - `ENFORCE` — the same requests get a 429.
 *
 * The IP is the first `X-Forwarded-For` hop. That is only the real visitor when the ingress
 * receives it from the load balancer (PROXY protocol); behind a SNAT'ing node it is the node, one
 * bucket covers everyone, and the filter must stay in shadow. Alpha had to switch it off for that
 * reason until the ingress was fixed.
 *
 * Counts live in this pod's memory, so with N replicas the effective limit is up to N times
 * [Rule.maxRequests]. Size limits against real traffic: Alpha's are 5 to 20 per minute after six
 * months of prod peaked at 3 per minute for any tenant.
 *
 * Not a Spring bean by itself; register it as one with the app's own [rules].
 */
open class PublicRateLimitFilter(
    private val rules: List<Rule>,
    private val mode: Mode = Mode.SHADOW,
    private val windowMillis: Long = 60_000L,
    private val clock: () -> Long = System::currentTimeMillis,
) : OncePerRequestFilter() {

    enum class Mode { OFF, SHADOW, ENFORCE;

        companion object {
            /** Parses `off`, `shadow` or `enforce` from configuration. */
            fun parse(value: String): Mode = valueOf(value.trim().uppercase())
        }
    }

    /**
     * A limited endpoint: [pattern] matched against the whole request URI. [scopeGroup] names the
     * regex group (e.g. the tenant slug) that keys the bucket next to the IP, so one tenant's
     * busy office cannot use up another tenant's form.
     */
    data class Rule(val pattern: Regex, val maxRequests: Int, val scopeGroup: Int = 0)

    private val logger = KotlinLogging.logger {}
    private val requestLog = ConcurrentHashMap<String, CopyOnWriteArrayList<Long>>()

    @Volatile
    private var lastCleanup = clock()

    /** The rule that applies to a POST at [path] and its scope value, or null. */
    fun ruleFor(path: String): Pair<Rule, String>? {
        for (rule in rules) {
            val match = rule.pattern.matchEntire(path) ?: continue
            val scope = if (rule.scopeGroup > 0) match.groupValues[rule.scopeGroup] else ""
            return rule to scope
        }
        return null
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val path = request.requestURI
        val matched = if (mode != Mode.OFF && request.method == "POST") ruleFor(path) else null
        if (matched == null) {
            filterChain.doFilter(request, response)
            return
        }
        val (rule, scope) = matched

        val ip = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()?.ifEmpty { null } ?: request.remoteAddr
        val key = "$ip:${rule.pattern.pattern}:$scope"
        val now = clock()

        cleanupIfNeeded(now)

        val timestamps = requestLog.computeIfAbsent(key) { CopyOnWriteArrayList() }
        timestamps.removeIf { it < now - windowMillis }

        if (timestamps.size >= rule.maxRequests) {
            logger.warn {
                "public rate limit exceeded (${mode.name.lowercase()}): ip=$ip path=$path scope=${scope.ifEmpty { "-" }} " +
                    "count=${timestamps.size + 1} limit=${rule.maxRequests}/${windowMillis / 1000}s"
            }
            if (mode == Mode.SHADOW) {
                timestamps.add(now)
                filterChain.doFilter(request, response)
                return
            }
            response.status = 429
            response.contentType = "application/json"
            response.writer.write("""{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Please try again later."}""")
            return
        }

        timestamps.add(now)
        filterChain.doFilter(request, response)
    }

    private fun cleanupIfNeeded(now: Long) {
        if (now - lastCleanup < CLEANUP_INTERVAL_MS) return
        lastCleanup = now
        val cutoff = now - windowMillis
        requestLog.entries.removeIf { (_, timestamps) ->
            timestamps.removeIf { it < cutoff }
            timestamps.isEmpty()
        }
    }

    private companion object {
        const val CLEANUP_INTERVAL_MS = 300_000L
    }
}
