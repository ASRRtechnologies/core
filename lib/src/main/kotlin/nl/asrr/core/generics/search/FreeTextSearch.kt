/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.generics.search

import org.springframework.data.mongodb.core.query.Criteria
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Builds a Mongo criteria for the generic free-text `?search=` used by the CRUD `getPage` endpoint.
 *
 * The CRUD services previously searched via [org.springframework.data.mongodb.core.query.TextCriteria],
 * which emits a MongoDB `$text` query. `$text` requires a text index on the collection; entities that
 * declare none fail at runtime with Mongo error 27 `IndexNotFound: 'text index required for $text
 * query'`, surfacing as an unhandled exception → HTTP 500 on any search.
 *
 * A per-field, case-insensitive regex needs no index: every whitespace-separated word must appear
 * within a single field (AND within a field), OR-ed across fields.
 */
object FreeTextSearch {
    private val searchableFieldsCache = ConcurrentHashMap<Class<*>, List<String>>()
    private val WHITESPACE = "\\s+".toRegex()

    /**
     * Regex criteria matching [search] against the entity's human-text String fields, or `null` when
     * there is nothing to search (blank input or the entity exposes no searchable text field) — in
     * which case the caller should apply no text filter.
     */
    fun criteria(search: String, entityClass: Class<*>): Criteria? {
        if (search.isBlank()) return null
        val words = search.trim().split(WHITESPACE).filter { it.isNotBlank() }
        if (words.isEmpty()) return null

        val fields = searchableFields(entityClass)
        if (fields.isEmpty()) return null

        val pattern =
            Pattern.compile(
                words.joinToString("") { "(?=.*${Pattern.quote(it)})" } + ".*",
                Pattern.CASE_INSENSITIVE,
            )
        val perField = fields.map { Criteria(it).regex(pattern) }
        return if (perField.size == 1) perField.first() else Criteria().orOperator(*perField.toTypedArray())
    }

    /**
     * The String fields worth searching: the entity's own text fields, excluding identity (`id`),
     * foreign-key references (`*Id`, e.g. customerId/ownerId) and audit fields (`*By`, e.g. createdBy).
     * Those hold opaque values that never match a human search term and only widen the query.
     */
    private fun searchableFields(entityClass: Class<*>): List<String> =
        searchableFieldsCache.getOrPut(entityClass) {
            generateSequence(entityClass) { it.superclass }
                .flatMap { it.declaredFields.asSequence() }
                .filter { it.type == String::class.java && !it.isSynthetic }
                .map { it.name }
                .filter { it != "id" && !it.endsWith("Id") && !it.endsWith("By") }
                .distinct()
                .toList()
        }
}
