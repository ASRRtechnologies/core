/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.auth.apikey

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyCodecTest {

    private val codec = ApiKeyCodec("alpha")

    @Test
    fun `generated keys are well formed and hash deterministically`() {
        val key = codec.generate()
        assertTrue(key.matches(Regex("alpha_[0-9a-f]{40}")))
        assertNull(codec.formatProblem(key))
        assertEquals(codec.hash(key), codec.hash(key))
        assertEquals(64, codec.hash(key).length)
        assertEquals(key.take(12), codec.display(key))
    }

    @Test
    fun `typos, other prefixes and truncation are rejected`() {
        val key = codec.generate()
        assertEquals("malformed", codec.formatProblem(key.replace("alpha_", "beta_")))
        assertEquals("malformed", codec.formatProblem("alpha"))
        val flipped = key.dropLast(1) + (if (key.last() == '0') '1' else '0')
        assertEquals("bad checksum", codec.formatProblem(flipped))
        assertEquals("bad checksum", codec.formatProblem(key.take(38)))
        assertThrows<IllegalArgumentException> { codec.display("nope") }
    }

    @Test
    fun `a key issued with an unpadded checksum is still valid`() {
        // Find a body whose CRC32 has a leading zero and strip it, as old keys did.
        val padded = generateSequence { codec.generate() }.first { it.substring(38, 39) == "0" }
        val unpadded = padded.substring(0, 38) + padded.substring(38).trimStart('0')
        assertNull(codec.formatProblem(unpadded))
    }
}
