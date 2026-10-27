/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.crypto

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.Document
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent

/**
 * Encrypts a fixed set of secret fields at rest, transparently for the services that use them.
 *
 * Threading encrypt/decrypt through every read and write site is how secrets get missed: in Alpha
 * the integrations did, the calendar OAuth tokens and a tenant's own Azure app secret did not, so a
 * database dump handed out live Google/Microsoft refresh tokens. This listener works on the BSON
 * document instead: secrets are encrypted on the way out ([onBeforeSave]) and decrypted on the way
 * in ([onAfterLoad]), so the Kotlin entities hold plaintext in memory only.
 *
 * [secretFields] maps a collection to its secret fields: document keys, or dotted paths into nested
 * documents (`settings.shopSettings.apiKey`). Only list fields that are never queried on, since
 * the stored value is ciphertext.
 *
 * Migration is lazy: a value that is not in the `v1:` format is treated as legacy plaintext and
 * returned as-is; the next save encrypts it. Without an encryption key (local dev) the listener
 * does nothing, loudly.
 *
 * Not a Spring bean by itself; declare one: `@Bean fun secretFields(e: EncryptionService) = SecretFieldEncryptionListener(e, mapOf("user" to setOf("totpSecret")))`.
 */
open class SecretFieldEncryptionListener(
    private val encryptionService: EncryptionService,
    val secretFields: Map<String, Set<String>>,
) : AbstractMongoEventListener<Any>() {
    private val logger = KotlinLogging.logger {}

    override fun onBeforeSave(event: BeforeSaveEvent<Any>) {
        val collection = event.collectionName ?: return
        val fields = secretFields[collection] ?: return
        val document = event.document ?: return
        transform(document, fields, collection) { value ->
            if (encryptionService.isEncrypted(value)) value else encryptionService.encrypt(value)
        }
    }

    override fun onAfterLoad(event: AfterLoadEvent<Any>) {
        val collection = event.collectionName ?: return
        val fields = secretFields[collection] ?: return
        val document = event.document ?: return
        transform(document, fields, collection) { value ->
            if (encryptionService.isEncrypted(value)) encryptionService.decrypt(value) else value
        }
    }

    private fun transform(document: Document, fields: Set<String>, collection: String, fn: (String) -> String) {
        if (!encryptionService.isConfigured()) {
            logger.warn { "Encryption key is not configured; secret fields of '$collection' are stored/read as-is" }
            return
        }
        for (field in fields) {
            val path = field.split('.')
            // Walk to the document that holds the last key; a record without the nested part has none.
            val holder = path.dropLast(1).fold(document as Document?) { current, key -> current?.get(key) as? Document } ?: continue
            val value = holder[path.last()] as? String ?: continue
            if (value.isEmpty()) continue
            holder[path.last()] = fn(value)
        }
    }
}
