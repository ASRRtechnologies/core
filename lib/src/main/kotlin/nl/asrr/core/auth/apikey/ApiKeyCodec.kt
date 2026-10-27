/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.auth.apikey

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID
import java.util.zip.CRC32

/**
 * The format of an API key: `<prefix>_<32 hex><8 hex CRC32>`, e.g. `alpha_3f2c…9a1b0c7e`.
 *
 * The checksum lets a typo or a truncated paste be rejected without a database lookup, and the
 * prefix lets secret scanners (gitleaks, GitHub) recognise a leaked key. Only [hash] is stored;
 * [display] is the part shown in a key list.
 *
 * Keys Alpha issued before the checksum was zero-padded carry it without leading zeros (one in
 * sixteen keys); those are still valid here.
 */
open class ApiKeyCodec(val prefix: String) {
    init {
        require(prefix.isNotBlank() && '_' !in prefix) { "API key prefix must be non-blank and contain no underscore" }
    }

    fun generate(): String {
        val body = UUID.randomUUID().toString().replace("-", "")
        return "${prefix}_$body${checksum(body)}"
    }

    fun isValidFormat(key: String): Boolean = formatProblem(key) == null

    /**
     * Why [key] is not a well-formed API key, or null when it is. Returned instead of logged: the
     * caller knows the request path, so it can say what was wrong in the one WARN that matters.
     */
    fun formatProblem(key: String): String? {
        val parts = key.split("_")
        if (parts.size != 2 || parts[0] != prefix || parts[1].isBlank()) return "malformed"
        val value = parts[1]
        val body = value.take(BODY_LENGTH)
        val check = value.drop(BODY_LENGTH)
        val expected = checksum(body)
        if (check.isEmpty() || (check != expected && check != expected.trimStart('0'))) return "bad checksum"
        return null
    }

    /** SHA-256 of the whole key, hex. What is stored and looked up; the key itself never is. */
    fun hash(key: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.toByteArray(StandardCharsets.UTF_8)))

    /** The prefix and the first six characters: enough to tell keys apart in a list, useless to an attacker. */
    fun display(key: String): String {
        val parts = key.split("_")
        require(parts.size == 2 && parts[0] == prefix && parts[1].isNotBlank()) { "API key format exception" }
        return "${parts[0]}_${parts[1].take(6)}"
    }

    private fun checksum(body: String): String {
        val crc = CRC32().apply { update(body.toByteArray(StandardCharsets.UTF_8)) }
        // Always eight characters: an unpadded checksum made one key in sixteen fail its own check.
        return "%08x".format(crc.value)
    }

    private companion object {
        const val BODY_LENGTH = 32
    }
}
