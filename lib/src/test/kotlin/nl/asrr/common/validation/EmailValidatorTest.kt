package nl.asrr.common.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EmailValidatorTest {

    @Test
    fun `test valid email`() {
        assertTrue(EmailValidator.isValid("me@me.com"))
    }

    @Test
    fun `test invalid email`() {
        assertFalse(EmailValidator.isValid("me@me"))
    }

}