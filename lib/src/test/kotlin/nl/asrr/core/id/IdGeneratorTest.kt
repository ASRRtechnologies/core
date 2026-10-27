package nl.asrr.core.id

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IdGeneratorTest {

    private val generator = IdGenerator()

    @Test
    fun `an id is an A and 15 base62 characters`() {
        repeat(10_000) {
            val id = generator.generate()
            assertTrue(id.matches(Regex("^A[0-9A-Za-z]{15}$")), "unexpected id '$id'")
        }
    }

    /**
     * The old base64url ids could carry `-` and `_`, also twice in a row or at the end, which is
     * awkward in URLs, CLI arguments and visible codes cut from an id.
     */
    @Test
    fun `never contains a dash or an underscore`() {
        repeat(100_000) {
            val id = generator.generate()
            assertTrue('-' !in id && '_' !in id, "unexpected id '$id'")
        }
    }

    /**
     * The old ids were milliseconds, a pod hash, a counter and only 8 random bits: one id told you
     * most of the next. Every character after the `A` is now random, so the alphabet is used evenly
     * at every position.
     */
    @Test
    fun `every position after the prefix is uniformly random`() {
        val counts = Array(IdGenerator.LENGTH) { IntArray(128) }
        val samples = 62_000
        repeat(samples) { generator.generate().forEachIndexed { i, c -> counts[i][c.code]++ } }

        for (position in 1 until IdGenerator.LENGTH) {
            val used = counts[position].count { it > 0 }
            assertEquals(62, used, "position $position uses $used of 62 characters")
            val expected = samples / 62.0
            counts[position].filter { it > 0 }.forEach {
                assertTrue(it in (expected * 0.75).toInt()..(expected * 1.25).toInt(), "position $position is skewed: $it vs ~$expected")
            }
        }
    }

    @Test
    fun `is unique across threads`() {
        val ids = Collections.synchronizedSet(HashSet<String>())
        val pool = Executors.newFixedThreadPool(8)
        repeat(8) { pool.submit { repeat(50_000) { ids.add(generator.generate()) } } }
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))

        assertEquals(400_000, ids.size)
    }
}
