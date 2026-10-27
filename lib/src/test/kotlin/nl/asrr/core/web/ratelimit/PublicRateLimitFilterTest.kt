/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.web.ratelimit

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals

class PublicRateLimitFilterTest {

    private val rules = listOf(PublicRateLimitFilter.Rule(Regex("^/public/leads/([^/]+)/submit$"), maxRequests = 2, scopeGroup = 1))
    private var now = 0L

    private fun post(filter: PublicRateLimitFilter, path: String, ip: String = "1.2.3.4"): Int {
        val request = MockHttpServletRequest("POST", path).apply { addHeader("X-Forwarded-For", "$ip, 10.0.0.1") }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response.status
    }

    @Test
    fun `enforce answers 429 past the limit, per ip and per tenant`() {
        val filter = PublicRateLimitFilter(rules, PublicRateLimitFilter.Mode.ENFORCE) { now }
        assertEquals(200, post(filter, "/public/leads/acme/submit"))
        assertEquals(200, post(filter, "/public/leads/acme/submit"))
        assertEquals(429, post(filter, "/public/leads/acme/submit"))
        assertEquals(200, post(filter, "/public/leads/other/submit"))
        assertEquals(200, post(filter, "/public/leads/acme/submit", ip = "5.6.7.8"))
        now = 60_001
        assertEquals(200, post(filter, "/public/leads/acme/submit"))
    }

    @Test
    fun `shadow and off let everything through`() {
        val shadow = PublicRateLimitFilter(rules, PublicRateLimitFilter.Mode.SHADOW) { now }
        val off = PublicRateLimitFilter(rules, PublicRateLimitFilter.Mode.parse(" off ")) { now }
        repeat(5) {
            assertEquals(200, post(shadow, "/public/leads/acme/submit"))
            assertEquals(200, post(off, "/public/leads/acme/submit"))
        }
    }

    @Test
    fun `unlisted paths and other methods are not counted`() {
        val filter = PublicRateLimitFilter(rules, PublicRateLimitFilter.Mode.ENFORCE) { now }
        repeat(5) { assertEquals(200, post(filter, "/api/projects")) }
        val get = MockHttpServletResponse()
        repeat(5) { filter.doFilter(MockHttpServletRequest("GET", "/public/leads/acme/submit"), get, MockFilterChain()) }
        assertEquals(200, get.status)
    }
}
