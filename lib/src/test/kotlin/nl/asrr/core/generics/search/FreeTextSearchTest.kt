/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.generics.search

import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

class FreeTextSearchTest {

    // A stand-in entity with the field shapes the reflection rule has to handle: human-text fields,
    // a foreign-key ref (*Id), an audit field (*By), the identity field, and a non-String field.
    @Suppress("unused")
    private class SampleEntity(
        val id: String,
        val tenantId: String,
        val number: String,
        val name: String,
        val description: String?,
        val customerId: String,
        val createdBy: String,
        val value: Double?,
    )

    private fun keysDeep(doc: Document): Set<String> = buildSet {
        doc.forEach { (k, v) ->
            add(k)
            when (v) {
                is Document -> addAll(keysDeep(v))
                is List<*> -> v.filterIsInstance<Document>().forEach { addAll(keysDeep(it)) }
            }
        }
    }

    @Test
    fun `blank search yields no criteria`() {
        assertNull(FreeTextSearch.criteria("", SampleEntity::class.java))
        assertNull(FreeTextSearch.criteria("   ", SampleEntity::class.java))
    }

    @Test
    fun `search never emits a text query and matches each field with a regex Pattern`() {
        val doc = FreeTextSearch.criteria("kitchen", SampleEntity::class.java)!!.criteriaObject
        assertFalse(keysDeep(doc).contains("\$text"), "must not use \$text (needs a text index that doesn't exist)")
        val clauses = (doc["\$or"] as List<*>).filterIsInstance<Document>()
        assertTrue(clauses.isNotEmpty())
        clauses.forEach { clause ->
            val value = clause.values.first()
            assertTrue(value is Pattern, "field ${clause.keys.first()} should match via regex, was ${value?.javaClass}")
            assertTrue((value as Pattern).flags() and Pattern.CASE_INSENSITIVE != 0, "regex should be case-insensitive")
        }
    }

    @Test
    fun `only human-text fields are searched, not id foreign-key or audit fields`() {
        val doc = FreeTextSearch.criteria("kitchen", SampleEntity::class.java)!!.criteriaObject
        val fields = (doc["\$or"] as List<*>).filterIsInstance<Document>().flatMap { it.keys }.toSet()
        assertEquals(setOf("number", "name", "description"), fields)
    }

    @Test
    fun `each word must appear within a single field (AND within field)`() {
        val doc = FreeTextSearch.criteria("walk in shower", SampleEntity::class.java)!!.criteriaObject
        val regex = (((doc["\$or"] as List<*>).first() as Document).values.first() as Pattern).pattern()
        listOf("walk", "in", "shower").forEach { word ->
            assertTrue(regex.contains("(?=.*${Pattern.quote(word)})"), "expected lookahead for '$word' in: $regex")
        }
    }
}
