package nl.asrr.core.id

import java.security.SecureRandom

/**
 * A threadsafe generator of unique ids: an `A` followed by 15 random base62 characters
 * (`0-9A-Za-z`), 16 characters in all and about 89 random bits.
 *
 * It used to encode milliseconds, a 12-bit machine hash, a counter and 8 random bits as base64url.
 * Those ids were close enough to guess from one another that a public endpoint looking something up
 * by id handed it out, and they could contain `-` and `_` (also doubled, or at the end). Ids made
 * the old way stay valid: they are opaque strings and nothing decodes them. They all start with
 * `AAAB`, and the chance that a new id equals an existing one is that of guessing 15 random
 * characters.
 *
 * Collisions: with 62^15 possible ids, a billion of them collide with a probability of about one in
 * 1.5 billion. Note that a Spring Data `save` of a colliding id would overwrite, not fail.
 */
class IdGenerator {
    private val random = SecureRandom()

    /**
     * Generates a unique id.
     *
     * @return an unique id
     */
    fun generate(): String {
        val id = CharArray(LENGTH)
        id[0] = PREFIX
        for (i in 1 until LENGTH) {
            id[i] = ALPHABET[random.nextInt(ALPHABET.length)]
        }
        return String(id)
    }

    companion object {
        const val LENGTH = 16
        const val PREFIX = 'A'
        const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    }
}
