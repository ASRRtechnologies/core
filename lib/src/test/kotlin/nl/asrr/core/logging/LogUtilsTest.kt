/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.logging

import nl.asrr.core.ops.migration.MigrationLog
import org.junit.jupiter.api.Test
import java.io.EOFException
import java.nio.channels.ClosedChannelException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogUtilsTest {

    @Test
    fun `throttle lets a key through once per window without sliding it`() {
        var now = 0L
        val throttle = LogThrottle(1_000) { now }
        assertTrue(throttle.shouldLog("a"))
        now = 600
        assertFalse(throttle.shouldLog("a"))
        assertTrue(throttle.shouldLog("b"))
        now = 1_000
        assertTrue(throttle.shouldLog("a"))
    }

    @Test
    fun `e-mail addresses are masked`() {
        assertEquals("j**e@example.com", maskEmail("jane@example.com"))
        assertEquals("**@x.nl", maskEmail("ab@x.nl"))
        assertEquals("***", maskEmail("not-an-address"))
    }

    @Test
    fun `client disconnects are recognised through the cause chain`() {
        assertTrue(isClientDisconnect(RuntimeException("write failed", EOFException())))
        assertTrue(isClientDisconnect(ClosedChannelException()))
        assertFalse(isClientDisconnect(IllegalStateException("bug")))
    }

    class BackfillPermissionsMigration

    @Test
    fun `migration lines carry the class name, status, count and keep reason`() {
        assertEquals(
            "migration=BackfillPermissionsMigration status=applied changed=3 - set defaults",
            MigrationLog.line(BackfillPermissionsMigration(), "applied", "set defaults", changed = 3),
        )
        assertEquals(
            "migration=BackfillPermissionsMigration status=noop keep=\"only index\" - nothing to do",
            MigrationLog.line(BackfillPermissionsMigration(), "noop", "nothing to do", keep = "only index"),
        )
        assertEquals("FooMigration", MigrationLog.nameOf("FooMigration\$\$SpringCGLIB\$\$0"))
    }
}
