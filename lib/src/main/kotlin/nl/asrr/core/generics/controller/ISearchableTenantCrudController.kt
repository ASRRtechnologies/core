/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.generics.controller

import io.swagger.v3.oas.annotations.Operation
import nl.asrr.core.generics.model.ITenantCrudEntity
import nl.asrr.core.generics.search.DynamicSearch
import nl.asrr.core.generics.search.SearchRequest
import nl.asrr.core.generics.service.ITenantCrudService
import org.springframework.data.domain.Page
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

/**
 * An [ITenantCrudController] with the dynamic list search: `GET`/`POST /dynamic-search` and
 * `GET /csv-export` (see [DynamicSearch] for the syntax). A separate base class, so an app that
 * already maps those paths itself does not get a second handler on the same route, which only
 * shows when the application boots.
 *
 * Like every endpoint, give the subclass's mappings a `@PreAuthorize`.
 */
abstract class ISearchableTenantCrudController<T : ITenantCrudEntity>(
    override val service: ITenantCrudService<T>,
) : ITenantCrudController<T>(service) {

    @PostMapping("/dynamic-search", produces = ["application/json"])
    @Operation(summary = "Search the current tenant with filters, per-field search, sort and paging")
    open fun dynamicSearchPost(@RequestBody request: SearchRequest): ResponseEntity<Page<T>> = ResponseEntity.ok(service.dynamicSearch(request))

    @GetMapping("/dynamic-search", produces = ["application/json"])
    @Operation(summary = "Search the current tenant: filters=field:value, field:in:A,B, field:null, field:gte:1; search=field:words; sort=field:desc;other")
    open fun dynamicSearch(
        @RequestParam(required = false) search: List<String> = emptyList(),
        @RequestParam(required = false) filters: List<String> = emptyList(),
        @RequestParam(required = false, defaultValue = "0") page: Int = 0,
        @RequestParam(required = false, defaultValue = "20") size: Int = 20,
        @RequestParam(required = false) sort: String? = null,
    ): ResponseEntity<Page<T>> = ResponseEntity.ok(service.dynamicSearch(DynamicSearch.parse(search, filters, page, size, sort)))

    @GetMapping("/csv-export")
    @Operation(summary = "The matches of a dynamic search as CSV")
    open fun csvExport(
        @RequestParam(required = false) search: List<String> = emptyList(),
        @RequestParam(required = false) filters: List<String> = emptyList(),
        @RequestParam(required = false) sort: String? = null,
    ): ResponseEntity<ByteArray> {
        val csv = service.dynamicSearchCsv(DynamicSearch.parse(search, filters, 0, DynamicSearch.MAX_PAGE_SIZE, sort))
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.csv\"")
            .contentType(MediaType("text", "csv"))
            .body(csv)
    }
}
