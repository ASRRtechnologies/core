package nl.asrr.common.auth.model

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RoleTest {

    @Test
    fun `enumContains should return true if enum value exists`() {
        assertTrue(enumContains<Role>("SUPER_ADMIN"))
    }

    @Test
    fun `enumContains should return false if enum value does not exist`() {
        assertFalse(enumContains<Role>("super_ADMIN"))
    }
}
