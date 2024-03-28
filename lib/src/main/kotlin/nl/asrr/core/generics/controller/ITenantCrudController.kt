/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.controller

import io.swagger.v3.oas.annotations.Operation
import nl.asrr.core.auth.service.ISecurityService
import nl.asrr.core.generics.model.ITenantCrudEntity
import nl.asrr.core.generics.service.ITenantCrudService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

abstract class ITenantCrudController<T : ITenantCrudEntity>(
    override val service: ITenantCrudService<T>, open val securityService: ISecurityService
) : ICrudController<T>(service) {

    // region TenantInjection
    @GetMapping("/find-all", produces = ["application/json"])
    @Operation(summary = "Find all for current tenant")
    override fun findAll(): ResponseEntity<List<T>> {
        return ResponseEntity.ok(service.findAll())
    }

    @GetMapping("/find/{id}", produces = ["application/json"])
    @Operation(summary = "Find by id in current tenant")
    override fun find(@PathVariable id: String): ResponseEntity<T> {
        return ResponseEntity.ok(service.find(id))
    }

    @GetMapping("/find", produces = ["application/json"])
    @Operation(summary = "Find by list of ids in current tenant")
    override fun findList(@RequestParam ids: List<String>): ResponseEntity<List<T>> {
        return ResponseEntity.ok(service.findList(ids))
    }

    @GetMapping("/page", produces = ["application/json"])
    @Operation(summary = "Get page by page number and size with optional sorting based on field to sortBy and direction")
    override fun getPage(
        @RequestParam pageNumber: Int,
        @RequestParam pageSize: Int?,
        @RequestParam sortBy: String?,
        @RequestParam direction: Sort.Direction?,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<Page<T>> {
        return ResponseEntity.ok(
            service.find(
                PageRequest.of(
                    pageNumber, pageSize ?: 50, direction ?: Sort.DEFAULT_DIRECTION, sortBy ?: "id"
                ), search ?: ""
            )
        )
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete by id in current tenant")
    override fun deleteById(@PathVariable id: String) {
        service.delete(id)
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    @Operation(summary = "Delete by list of ids in current tenant")
    override fun deleteList(@RequestBody ids: List<String>) {
        service.delete(ids)
    }
    // endregion TenantInjection

}
