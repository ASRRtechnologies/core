/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.monitoring

import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.protocol.Request
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonitoringTest {

    @Test
    fun `credentials are taken out of reported urls`() {
        assertEquals("/api/v1/auth/refresh/[Filtered]", RequestContextEventProcessor.safePath("/api/v1/auth/refresh/abc.def?x=1"))
        assertEquals("/unsubscribe/[Filtered]", RequestContextEventProcessor.safePath("/unsubscribe/${"a".repeat(40)}"))
        assertEquals("/projects/AAABoXhk9AzovyAN", RequestContextEventProcessor.safePath("/projects/AAABoXhk9AzovyAN#tab"))
        assertEquals("/files/123e4567-e89b-12d3-a456-426614174000", RequestContextEventProcessor.safePath("/files/123e4567-e89b-12d3-a456-426614174000"))
    }

    @Test
    fun `events are tagged with tenant and record id and stripped of headers`() {
        val processor = RequestContextEventProcessor { ReportingContext(tenantId = "t1", auth = "jwt", userRecordId = "u1") }
        val event = SentryEvent().apply {
            request = Request().apply {
                headers = mapOf("Authorization" to "Bearer x")
                queryString = "token=x"
                url = "/api/v1/auth/logout/tok"
            }
        }
        processor.process(event, Hint())
        assertEquals("t1", event.getTag("tenant"))
        assertEquals("jwt", event.getTag("auth"))
        assertEquals("u1", event.user?.id)
        assertNull(event.request?.headers)
        assertNull(event.request?.queryString)
        assertEquals("/api/v1/auth/logout/[Filtered]", event.request?.url)
    }

    @Test
    fun `a failing context still lets the event through`() {
        val event = SentryEvent()
        RequestContextEventProcessor { error("no security context") }.process(event, Hint())
        assertNull(event.getTag("tenant"))
    }

    @Test
    fun `sentry stays off without a dsn and names the release after the app`() {
        assertNull(SentryBootstrap.options("alpha-api", "1.0.0") { null })
        val options = SentryBootstrap.options("alpha-api", "4.2.0") { if (it == "SENTRY_DSN") "https://k@o.ingest.sentry.io/1" else null }!!
        assertEquals("alpha-api@4.2.0", options.release)
        assertNull(SentryBootstrap.options("alpha-api", "unknown") { if (it == "SENTRY_DSN") "https://k@o.ingest.sentry.io/1" else null }!!.release)
        assertEquals(0.0, SentryBootstrap.sampleRate("GET /actuator/health", 0.5))
    }

    enum class Jobs(
        override val slug: String,
        override val schedule: CronMonitor.Schedule,
        override val marginMinutes: Long,
        override val maxRuntimeMinutes: Long,
    ) : CronMonitor {
        NIGHTLY("nightly-import", CronMonitor.Schedule.Cron("15 3 * * *"), 30, 120),
    }

    @Test
    fun `cron monitor config follows the job and runs the block without sentry`() {
        val config = Jobs.NIGHTLY.monitorConfig()
        assertEquals(30L, config.checkinMargin)
        assertEquals(120L, config.maxRuntime)
        assertEquals(2L, config.failureIssueThreshold)
        assertEquals(42, Jobs.NIGHTLY.run { 42 })
    }
}
