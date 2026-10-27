/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.generics.search

import org.bson.Document
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.mongodb.core.query.Criteria
import java.time.Instant
import java.util.regex.Pattern
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicSearchTest {

    private fun filterDocument(vararg filters: String): Document =
        DynamicSearch.criteria(DynamicSearch.parse(emptyList(), filters.toList(), 0, 20, null))!!.criteriaObject

    @Test
    fun `equality, in, null and range filters`() {
        val doc = filterDocument("status:in:OPEN,PARTIAL", "shop.collectedAt:null", "total:gte:100", "total:lt:200", "paid:true")
        assertEquals(listOf("OPEN", "PARTIAL"), (doc["status"] as Document)["\$in"])
        assertTrue(doc.containsKey("shop.collectedAt") && doc["shop.collectedAt"] == null)
        assertEquals(Document("\$gte", 100).append("\$lt", 200), doc["total"])
        assertEquals(true, doc["paid"])
    }

    @Test
    fun `an IN split apart by Spring's list binding is put back together`() {
        val doc = filterDocument("status:OPEN", "PARTIAL", "CLOSED")
        assertEquals(listOf("OPEN", "PARTIAL", "CLOSED"), (doc["status"] as Document)["\$in"])
    }

    @Test
    fun `two filters on one field keep the last, so several values need in`() {
        assertEquals("CLOSED", filterDocument("status:OPEN", "status:CLOSED")["status"])
    }

    @Test
    fun `iso dates are compared as instants`() {
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), (filterDocument("created:gte:2026-10-01")["created"] as Document)["\$gte"])
    }

    @Test
    fun `search terms require every word in the field and OR across entries`() {
        val doc = DynamicSearch.criteria(SearchRequest(search = listOf("name:jan de", "email:jan")))!!.criteriaObject
        val clauses = doc["\$or"] as List<*>
        assertEquals(2, clauses.size)
        val name = (clauses[0] as Document)["name"] as Pattern
        assertTrue(name.matcher("De Jan Bakker").matches())
        assertTrue(!name.matcher("Jan Bakker").matches())
    }

    @Test
    fun `operator keys and javascript never reach the query`() {
        assertThrows<IllegalArgumentException> { DynamicSearch.criteria(SearchRequest(filters = mapOf("\$where" to "sleep(1000)"))) }
        assertThrows<IllegalArgumentException> { DynamicSearch.criteria(SearchRequest(search = listOf("a.\$b:x"))) }
        assertThrows<IllegalArgumentException> { DynamicSearch.query(SearchRequest(sort = listOf(SortField("\$natural")))) }
    }

    @Test
    fun `page size is bounded`() {
        assertThrows<IllegalArgumentException> { DynamicSearch.query(SearchRequest(size = 0)) }
        assertThrows<IllegalArgumentException> { DynamicSearch.query(SearchRequest(size = DynamicSearch.MAX_PAGE_SIZE + 1)) }
    }

    @Test
    fun `query ANDs the scope with the request and pages`() {
        val request = DynamicSearch.parse(emptyList(), listOf("status:OPEN", "page:3"), 2, 25, "created:desc;name")
        val query = DynamicSearch.query(request, Criteria.where("tenantId").`is`("t1"))
        assertEquals(
            Document("\$and", listOf(Document("tenantId", "t1"), Document("status", "OPEN"))),
            query.queryObject,
        )
        assertEquals(Document("created", -1).append("name", 1), query.sortObject)
        assertEquals(50, query.skip)
        assertEquals(25, query.limit)
    }

    @Test
    fun `an empty request has no criteria`() {
        assertNull(DynamicSearch.criteria(SearchRequest()))
        assertTrue(DynamicSearch.countQuery(SearchRequest()).queryObject.isEmpty())
    }
}
