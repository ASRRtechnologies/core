/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.generics.search

import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.regex.Pattern

/**
 * One list query: equality and range [filters], per-field [search] terms, [sort] and a page.
 *
 * Filter keys are document paths, optionally ending in a range operator (`invoiceDate>=`); a value
 * with commas is an IN, the value `null` matches a missing or null field. [search] entries are
 * `field:words`: every word must occur in that field (case-insensitive), and the entries are OR-ed.
 */
data class SearchRequest(
    val filters: Map<String, Any?> = emptyMap(),
    val sort: List<SortField> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val search: List<String> = emptyList(),
)

data class SortField(
    val field: String,
    val ascending: Boolean = true,
)

/**
 * The dynamic list search of the generic CRUD layer, as a query builder that knows nothing about
 * tenants: [ITenantCrudService.search][nl.asrr.core.generics.service.ITenantCrudService.search]
 * adds the tenant, an app adds its own scoping next to it.
 *
 * Query-string syntax ([parse]), as the frontends send it:
 * - `filters=status:OPEN` equality; `filters=status:in:OPEN,PARTIALLY_COMPLETED` IN;
 *   `filters=shop.collectedAt:null` missing or null; `filters=total:gte:100` (also `lte`, `gt`, `lt`);
 * - `search=name:jan de` every word in `name`; several `search=` are OR-ed;
 * - `sort=created:desc;name` (ascending by default).
 *
 * Two `filters=` on the same field do not mean "either": the map keeps the last one. Use `:in:` for
 * several values. A list that showed two open orders on its tiles stayed empty for exactly that.
 *
 * Every field name comes from the client and goes into `Criteria`/`Sort` as a key, so
 * [validateFieldPath] rejects anything but a plain dotted path: Mongo treats a `$`-key as an
 * operator, and `$where` runs JavaScript on the server where it is enabled.
 */
object DynamicSearch {

    /** Upper bound for one search page; `size=0` meant `limit(0)` = unlimited in Mongo. */
    const val MAX_PAGE_SIZE = 10_000

    /** Query parameters that steer paging, not filters on a field of the same name. */
    val RESERVED_FILTERS = setOf("page", "size", "sort", "includeDeleted")

    /** A plain (dotted) document path: letters, digits, underscores, dots. No `$` operators. */
    private val FIELD_PATH = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)*")

    private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})?)?""")

    fun validateFieldPath(field: String): String {
        require(FIELD_PATH.matches(field)) { "Invalid field name '$field'" }
        return field
    }

    fun validatePageSize(size: Int): Int {
        require(size in 1..MAX_PAGE_SIZE) { "size must be between 1 and $MAX_PAGE_SIZE" }
        return size
    }

    /** A filter value as Mongo should compare it: null, booleans, numbers and ISO dates are typed. */
    fun coerceValue(value: String): Any? = when {
        value.equals("null", true) -> null
        value.equals("true", true) -> true
        value.equals("false", true) -> false
        value.toIntOrNull() != null -> value.toInt()
        value.toLongOrNull() != null -> value.toLong()
        value.toDoubleOrNull() != null -> value.toDouble()
        else -> parseIsoDateOrNull(value) ?: value
    }

    fun parseIsoDateOrNull(value: String): Instant? {
        if (!ISO_DATE.matches(value)) return null
        return runCatching {
            if (value.length == 10) LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant() else OffsetDateTime.parse(value).toInstant()
        }.getOrNull()
    }

    /** Builds a [SearchRequest] from the `search`, `filters`, `page`, `size` and `sort` query parameters. */
    fun parse(search: List<String>, filters: List<String>, page: Int, size: Int, sort: String?): SearchRequest {
        val sortFields = sort?.split(";")?.mapNotNull {
            val parts = it.split(":")
            val field = parts[0]
            if (field.isBlank()) return@mapNotNull null
            SortField(field, parts.getOrNull(1)?.lowercase() != "desc")
        } ?: emptyList()

        val filterFields = reattachCommaSplitFilters(filters).mapNotNull {
            val parts = it.split(":", limit = 3)
            if (parts.size < 2) return@mapNotNull null
            val field = parts[0]
            if (field.isBlank()) return@mapNotNull null
            // field:operator:value, e.g. "subsidiaryId:in:A,B" or "total:gte:100".
            val (key, value) = if (parts.size == 3) {
                val raw = parts[2]
                when (parts[1].lowercase()) {
                    "in" -> field to raw // comma-separated, handled in criteria()
                    "gte", ">=" -> "$field>=" to raw
                    "lte", "<=" -> "$field<=" to raw
                    "gt", ">" -> "$field>" to raw
                    "lt", "<" -> "$field<" to raw
                    else -> field to raw // unknown operator, treat as value
                }
            } else {
                field to parts[1]
            }
            if (value.isBlank()) null else key to value
        }.toMap()

        return SearchRequest(filters = filterFields, sort = sortFields, page = page, size = size, search = search)
    }

    /**
     * Re-joins comma-separated filter values that Spring's `List<String>` binding splits apart.
     *
     * `filters=status:A,B,C` is bound as `["status:A", "B", "C"]`: the comma is a list delimiter.
     * Distinct filters are sent as separate `filters=` params, so an entry without `field:` can only
     * be such a continuation; without this an IN filter silently collapses to its first value.
     */
    fun reattachCommaSplitFilters(filters: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (entry in filters) {
            if (entry.contains(":") || result.isEmpty()) result.add(entry) else result[result.lastIndex] = "${result.last()},$entry"
        }
        return result
    }

    /** The filter and search criteria of [request], or null when it has neither. */
    fun criteria(request: SearchRequest): Criteria? = listOfNotNull(filterCriteria(request.filters), searchCriteria(request.search)).let {
        when (it.size) {
            0 -> null
            1 -> it.first()
            else -> Criteria().andOperator(*it.toTypedArray())
        }
    }

    /**
     * A query for one page of [request], AND-ing [scope] (the tenant, and whatever the app restricts
     * to) with the request's own criteria. Use [countQuery] for the total: a count must not skip.
     */
    fun query(request: SearchRequest, vararg scope: Criteria): Query {
        val size = validatePageSize(request.size)
        require(request.page >= 0) { "page must be >= 0" }
        val query = countQuery(request, *scope)
        if (request.sort.isNotEmpty()) {
            query.with(Sort.by(request.sort.map { if (it.ascending) Sort.Order.asc(validateFieldPath(it.field)) else Sort.Order.desc(validateFieldPath(it.field)) }))
        }
        return query.skip(request.page.toLong() * size).limit(size)
    }

    fun countQuery(request: SearchRequest, vararg scope: Criteria): Query {
        val parts = scope.toList() + listOfNotNull(criteria(request))
        return when (parts.size) {
            0 -> Query()
            1 -> Query(parts.first())
            else -> Query(Criteria().andOperator(*parts.toTypedArray()))
        }
    }

    private data class Clause(val op: String, val raw: String, val coerced: Any?)

    private fun filterCriteria(filters: Map<String, Any?>): Criteria? {
        // Group by base field so a range (invoiceDate>= AND invoiceDate<=) chains operators on one
        // Criteria entry: `.and(field)` twice for the same field fails with "can't add a second
        // '<field>' expression".
        val byField = linkedMapOf<String, MutableList<Clause>>()
        filters.filterKeys { it !in RESERVED_FILTERS }.forEach { (field, value) ->
            val raw = value as? String ?: return@forEach
            val (base, op) = when {
                field.endsWith(">=") -> field.removeSuffix(">=") to ">="
                field.endsWith("<=") -> field.removeSuffix("<=") to "<="
                field.endsWith(">") -> field.removeSuffix(">") to ">"
                field.endsWith("<") -> field.removeSuffix("<") to "<"
                else -> field to "="
            }
            byField.getOrPut(base) { mutableListOf() }.add(Clause(op, raw, coerceValue(raw)))
        }
        if (byField.isEmpty()) return null

        val criteria = Criteria()
        byField.forEach { (field, clauses) ->
            val fieldCriteria = criteria.and(validateFieldPath(field))
            clauses.forEach { c ->
                when {
                    c.op == "=" && c.coerced == null -> fieldCriteria.`is`(null)
                    c.op == "=" && c.raw.contains(",") -> {
                        val values = c.raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map(::coerceValue)
                        if (values.isNotEmpty()) fieldCriteria.`in`(values)
                    }
                    c.op == "=" -> fieldCriteria.`is`(c.coerced)
                    c.op == ">=" -> c.coerced?.let { fieldCriteria.gte(it) }
                    c.op == "<=" -> c.coerced?.let { fieldCriteria.lte(it) }
                    c.op == ">" -> c.coerced?.let { fieldCriteria.gt(it) }
                    c.op == "<" -> c.coerced?.let { fieldCriteria.lt(it) }
                }
            }
        }
        return criteria
    }

    private fun searchCriteria(search: List<String>): Criteria? {
        val perField = search.mapNotNull { entry ->
            val (field, value) = entry.split(":", limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            if (value.isBlank()) return@mapNotNull null
            val words = value.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            val pattern = Pattern.compile(words.joinToString("") { "(?=.*${Pattern.quote(it)})" } + ".*", Pattern.CASE_INSENSITIVE)
            Criteria(validateFieldPath(field)).regex(pattern)
        }
        return when (perField.size) {
            0 -> null
            1 -> perField.first()
            else -> Criteria().orOperator(*perField.toTypedArray())
        }
    }
}
