/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.audit

import org.bson.Document
import org.bson.types.Decimal128
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Date

/** The changed fields of one write, cut off at [AuditChanges.MAX_CHANGES]. */
data class AuditChangeSet(
    val changes: List<AuditFieldChange>,
    val truncated: Int = 0,
)

/**
 * Turns Mongo writes into [AuditFieldChange]s. Works on BSON documents rather than Kotlin entities:
 * the documents are what is actually stored, are already in hand at the write (no reflection), and
 * mean one diff serves every collection.
 */
object AuditChanges {
    const val MASK = "***"
    const val MAX_CHANGES = 100
    const val MAX_VALUE_LENGTH = 300
    private const val MAX_FILTER_LENGTH = 500
    private const val MAX_LABEL_LENGTH = 120

    /**
     * Fields that change as a side effect of using the app, not as a decision anyone made. Matched
     * on the first path segment, so `sessions.$.lastActive` from a positional update is caught too.
     */
    private val BOOKKEEPING = setOf(
        "_class", "events", "updated", "updatedBy", "sessions", "lastActive", "lastLogin",
        "failedLoginAttempts", "lastFailedLoginAttempt",
    )

    /** Any path segment containing one of these is a secret: its change is logged, its value never. */
    private val SECRET_MARKERS = listOf("password", "secret", "token", "hash", "totp", "recoverycode", "apikey", "privatekey", "credential")

    private val LABEL_FIELDS = listOf("name", "title", "subject", "displayName", "username", "email", "number", "code")

    fun diff(before: Document, after: Document, skip: Set<String> = emptySet()): AuditChangeSet {
        val out = Collector()
        walk("", before, after, skip, out)
        return out.result()
    }

    fun fromUpdate(update: Document): AuditChangeSet {
        val out = Collector()
        for ((key, value) in update) {
            if (key.startsWith("$") && value is Document) {
                for ((field, v) in value) add(out, field, key, v)
            } else {
                // A replacement document rather than operators.
                add(out, key, null, value)
            }
        }
        return out.result()
    }

    fun updateAction(update: Document, multi: Boolean): AuditAction {
        if ((update["\$set"] as? Document)?.get("isDeleted") == true) return AuditAction.SOFT_DELETE
        if ((update["\$unset"] as? Document)?.containsKey("isDeleted") == true) return AuditAction.RESTORE
        return if (multi) AuditAction.BULK_UPDATE else AuditAction.UPDATE
    }

    /**
     * The id a query pins down: an equality, or an `$in` of exactly one id (how the list endpoints
     * delete a single record). Null for anything that can match several records.
     */
    fun entityId(query: Document): String? {
        val id = query["_id"] ?: query["id"]
        val single = ((id as? Document)?.get("\$in") as? Collection<*>)?.singleOrNull()
        return scalar(single ?: id)
    }

    fun tenantId(query: Document): String? = query["tenantId"] as? String

    fun label(doc: Document?): String? {
        if (doc == null) return null
        val name = LABEL_FIELDS.firstNotNullOfOrNull { (doc[it] as? String)?.takeIf(String::isNotBlank) }
            ?: listOfNotNull(doc["firstName"] as? String, doc["lastName"] as? String).joinToString(" ").takeIf(String::isNotBlank)
        return name?.let { cut(it, MAX_LABEL_LENGTH) }
    }

    fun renderFilter(query: Document): String = cut(json(query), MAX_FILTER_LENGTH)

    private fun add(out: Collector, field: String, op: String?, value: Any?) {
        if (isBookkeeping(field)) return
        val to = when {
            op == "\$unset" -> null
            isSecret(field) -> MASK
            else -> render(value)
        }
        out.add(AuditFieldChange(field, null, to, op))
    }

    private fun walk(path: String, a: Any?, b: Any?, skip: Set<String>, out: Collector) {
        if (path.isNotEmpty() && isSecret(path)) {
            if (!same(a, b)) out.add(AuditFieldChange(path, a?.let { MASK }, b?.let { MASK }))
            return
        }
        when {
            a is Document && b is Document -> {
                for (key in a.keys + b.keys.filterNot(a::containsKey)) {
                    val child = if (path.isEmpty()) key else "$path.$key"
                    if (path.isEmpty() && isBookkeeping(key)) continue
                    if (child in skip) continue
                    walk(child, a[key], b[key], skip, out)
                }
            }
            a is List<*> && b is List<*> && keyed(a) != null && keyed(b) != null -> {
                val before = keyed(a)!!
                val after = keyed(b)!!
                for (key in before.keys + after.keys.filterNot(before::containsKey)) {
                    walk("$path[$key]", before[key], after[key], skip, out)
                }
            }
            !same(a, b) -> out.add(AuditFieldChange(path, render(a), render(b)))
        }
    }

    /** List items by their id, or null when the list is not a list of identified documents. */
    private fun keyed(list: List<*>): Map<String, Document>? {
        if (list.isEmpty()) return emptyMap()
        val byKey = LinkedHashMap<String, Document>(list.size)
        for (item in list) {
            val doc = item as? Document ?: return null
            val key = scalar(doc["_id"] ?: doc["id"]) ?: return null
            byKey[key] = doc
        }
        return byKey
    }

    private fun same(a: Any?, b: Any?): Boolean = when {
        a is Number && b is Number -> number(a).compareTo(number(b)) == 0
        else -> a == b
    }

    private fun number(n: Number): BigDecimal = if (n is Decimal128) n.bigDecimalValue() else BigDecimal(n.toString())

    private fun scalar(v: Any?): String? = when (v) {
        is String -> v
        is ObjectId -> v.toHexString()
        is Number -> v.toString()
        else -> null
    }

    private fun isBookkeeping(path: String): Boolean = path.substringBefore('.').substringBefore('[') in BOOKKEEPING

    private fun isSecret(path: String): Boolean = path.split('.', '[').any { segment ->
        val s = segment.lowercase()
        SECRET_MARKERS.any { s.contains(it) }
    }

    private fun render(v: Any?): String? = when (v) {
        null -> null
        is String -> cut(v, MAX_VALUE_LENGTH)
        else -> cut(raw(v), MAX_VALUE_LENGTH)
    }

    private fun raw(v: Any?): String = when (v) {
        null -> "null"
        is String -> v
        is Date -> v.toInstant().toString()
        is ZonedDateTime -> v.toInstant().toString()
        is ObjectId -> v.toHexString()
        is Decimal128 -> v.bigDecimalValue().toPlainString()
        is Enum<*> -> v.name
        is Document -> json(v)
        is Collection<*> -> v.joinToString(", ", "[", "]") { raw(it) }
        else -> v.toString()
    }

    /** Query and update objects hold raw Java values the BSON codec may not know; fall back to toString. */
    private fun json(doc: Document): String = runCatching { doc.toJson() }.getOrElse { doc.toString() }

    private fun cut(s: String, max: Int): String = if (s.length <= max) s else s.take(max) + "…"

    private class Collector {
        private val changes = ArrayList<AuditFieldChange>()
        private var truncated = 0

        fun add(change: AuditFieldChange) {
            if (changes.size < MAX_CHANGES) changes += change else truncated++
        }

        fun result() = AuditChangeSet(changes, truncated)
    }
}
