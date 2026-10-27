/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Field-level encryption for secrets stored at rest (integration client secrets, OAuth refresh
 * tokens, TOTP secrets). AES-256-GCM with a random IV per value. Ciphertext is persisted as
 * `v1:` + base64(iv || ciphertext+tag) so the scheme can be rotated later without ambiguity.
 *
 * [base64Key] is base64 of 32 bytes, typically `@Value("\${app.encryption.key:}")`. It is validated
 * lazily: the app must boot in environments that don't (yet) exercise encryption, but any
 * encrypt/decrypt call without a valid key fails fast and loudly.
 *
 * Not a Spring bean by itself; declare one: `@Bean fun encryptionService(@Value("\${app.encryption.key:}") key: String) = EncryptionService(key)`.
 */
open class EncryptionService(private val base64Key: String) {
    private val random = SecureRandom()

    private val keySpec: SecretKeySpec by lazy {
        require(base64Key.isNotBlank()) {
            "The encryption key is not configured; cannot encrypt/decrypt secrets at rest"
        }
        val keyBytes = Base64.getDecoder().decode(base64Key.trim())
        require(keyBytes.size == KEY_SIZE_BYTES) {
            "The encryption key must decode to $KEY_SIZE_BYTES bytes (AES-256), got ${keyBytes.size}"
        }
        SecretKeySpec(keyBytes, "AES")
    }

    fun isConfigured(): Boolean = base64Key.isNotBlank()

    fun encrypt(plaintext: String): String {
        val iv = ByteArray(IV_SIZE_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            // nosemgrep: kotlin.lang.security.gcm-detection.gcm-detection -- fresh SecureRandom IV per encrypt()
            init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    fun decrypt(encrypted: String): String {
        require(encrypted.startsWith(PREFIX)) { "Encrypted value has an unknown format" }
        val combined = Base64.getDecoder().decode(encrypted.removePrefix(PREFIX))
        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = combined.copyOfRange(IV_SIZE_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            // nosemgrep: kotlin.lang.security.gcm-detection.gcm-detection -- IV read from the ciphertext
            init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    /** True if [value] looks like something this service produced. */
    fun isEncrypted(value: String?): Boolean = value != null && value.startsWith(PREFIX)

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BYTES = 32
        private const val IV_SIZE_BYTES = 12
        private const val TAG_SIZE_BITS = 128
        private const val PREFIX = "v1:"
    }
}
