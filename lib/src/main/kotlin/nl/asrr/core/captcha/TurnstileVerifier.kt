/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.captcha

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

/**
 * Verifies a Cloudflare Turnstile token from a public form (lead form, sign-up, booking widget).
 *
 * Without a [secretKey] verification is skipped and every token passes: that is local dev and the
 * e2e stack, which have no site key in the frontend either. In the clusters the secret is set, and
 * a missing or invalid token, or a failed call to Cloudflare, fails closed.
 *
 * Not a Spring bean by itself; declare one: `@Bean fun turnstile(@Value("\${cloudflare.turnstile.secret-key:}") key: String) = TurnstileVerifier(key)`.
 */
open class TurnstileVerifier(
    private val secretKey: String,
    private val restClient: RestClient = RestClient.create(),
    private val verifyUrl: String = VERIFY_URL,
) {
    private val logger = KotlinLogging.logger {}

    /** True when a secret is configured, i.e. verification is enforced. */
    val enabled: Boolean get() = secretKey.isNotBlank()

    fun verify(token: String?, remoteIp: String? = null): Boolean {
        if (!enabled) {
            logger.warn { "Turnstile secret key not configured; skipping verification" }
            return true
        }
        if (token.isNullOrBlank()) return false

        return try {
            // Form-encoded by the converter: a token can contain characters that would break a hand-built body.
            val form = LinkedMultiValueMap<String, String>().apply {
                add("secret", secretKey)
                add("response", token)
                remoteIp?.let { add("remoteip", it) }
            }
            val response = restClient.post()
                .uri(verifyUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TurnstileResponse::class.java)
            response?.success == true
        } catch (e: Exception) {
            logger.error(e) { "Turnstile verification request failed" }
            false
        }
    }

    data class TurnstileResponse(val success: Boolean = false)

    companion object {
        const val VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify"
    }
}
