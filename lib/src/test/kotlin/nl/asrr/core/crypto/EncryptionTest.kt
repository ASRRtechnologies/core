/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.crypto

import org.bson.Document
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EncryptionTest {

    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    private val service = EncryptionService(key)

    @Test
    fun `round trip with a fresh iv per value`() {
        val a = service.encrypt("refresh-token")
        val b = service.encrypt("refresh-token")
        assertTrue(a.startsWith("v1:"))
        assertNotEquals(a, b)
        assertEquals("refresh-token", service.decrypt(a))
    }

    @Test
    fun `a missing or short key fails on use, not at boot`() {
        val unconfigured = EncryptionService("")
        assertEquals(false, unconfigured.isConfigured())
        assertThrows<IllegalArgumentException> { unconfigured.encrypt("x") }
        assertThrows<IllegalArgumentException> { EncryptionService(Base64.getEncoder().encodeToString(ByteArray(16))).encrypt("x") }
    }

    @Test
    fun `listener encrypts listed fields on save, nested paths too, and decrypts on load`() {
        val listener = SecretFieldEncryptionListener(service, mapOf("tenant" to setOf("clientSecret", "settings.shop.apiKey")))
        val document = Document("name", "Acme")
            .append("clientSecret", "s3cret")
            .append("settings", Document("shop", Document("apiKey", "k3y")))

        listener.onBeforeSave(BeforeSaveEvent(Any(), document, "tenant"))
        assertTrue(service.isEncrypted(document.getString("clientSecret")))
        val nested = (document["settings"] as Document)["shop"] as Document
        assertTrue(service.isEncrypted(nested.getString("apiKey")))
        assertEquals("Acme", document["name"])

        listener.onAfterLoad(AfterLoadEvent(document, Any::class.java, "tenant"))
        assertEquals("s3cret", document["clientSecret"])
        assertEquals("k3y", nested["apiKey"])
    }

    @Test
    fun `legacy plaintext is read as-is and other collections are untouched`() {
        val listener = SecretFieldEncryptionListener(service, mapOf("user" to setOf("totpSecret")))
        val legacy = Document("totpSecret", "PLAIN")
        listener.onAfterLoad(AfterLoadEvent(legacy, Any::class.java, "user"))
        assertEquals("PLAIN", legacy["totpSecret"])

        val other = Document("totpSecret", "PLAIN")
        listener.onBeforeSave(BeforeSaveEvent(Any(), other, "project"))
        assertEquals("PLAIN", other["totpSecret"])
    }
}
