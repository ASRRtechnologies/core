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
    override val service: ITenantCrudService<T>,
    open val securityService: ISecurityService
) : ICrudController<T>(service) {

    // region TenantInjection
    @GetMapping("/find-all", produces = ["application/json"])
    @Operation(summary = "Find all for current tenant")
    override fun findAll(): ResponseEntity<List<T>> {
        return ResponseEntity.ok(service.findAllByTenantId(securityService.getTenantId()))
    }

    @GetMapping("/find/{id}", produces = ["application/json"])
    @Operation(summary = "Find by id in current tenant")
    override fun find(@PathVariable id: String): ResponseEntity<T> {
        return ResponseEntity.ok(service.findOneByIdAndTenantId(securityService.getTenantId(), id))
    }

    @GetMapping("/find", produces = ["application/json"])
    @Operation(summary = "Find by list of ids in current tenant")
    override fun findList(@RequestParam ids: List<String>): ResponseEntity<List<T>> {
        return ResponseEntity.ok(service.findAllByIdAndTenantId(ids, securityService.getTenantId()))
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
                securityService.getTenantId(),
                PageRequest.of(
                    pageNumber,
                    pageSize ?: 50,
                    direction ?: Sort.DEFAULT_DIRECTION,
                    sortBy ?: "id"
                ), search ?: ""
            )
        )
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete by id in current tenant")
    override fun deleteById(@PathVariable id: String) {
        service.delete(securityService.getTenantId(), id)
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    @Operation(summary = "Delete by list of ids in current tenant")
    override fun deleteList(@RequestBody ids: List<String>) {
        service.deleteList(ids, securityService.getTenantId())
    }
    // endregion TenantInjection

    @GetMapping("/find-all/{tenantId}", produces = ["application/json"])
    @Operation(summary = "Find all by tenantId ")
    open fun findAllByTenantId(@PathVariable tenantId: String): ResponseEntity<List<ITenantCrudEntity>> {
        return ResponseEntity.ok(service.findAllByTenantId(tenantId))
    }

    @GetMapping("/tenant/find/{tenantId}/{id}", produces = ["application/json"])
    @Operation(summary = "Find by id and tenantId")
    open fun findByTenantIdAndId(
        @PathVariable tenantId: String,
        @PathVariable id: String,
    ): ResponseEntity<T> {
        return ResponseEntity.ok(service.findOneByIdAndTenantId(id, tenantId))
    }

    @GetMapping("/tenant/page/{tenantId}", produces = ["application/json"])
    @Operation(summary = "Get page by page number and size")
    open fun getTenantPage(
        @PathVariable tenantId: String,
        @RequestParam pageNumber: Int = 0,
        @RequestParam pageSize: Int? = 50,
        @RequestParam sortBy: String? = null,
        @RequestParam direction: Sort.Direction? = Sort.DEFAULT_DIRECTION,
        @RequestParam(required = false) search: String? = null,
    ): ResponseEntity<Page<T>> {
        return ResponseEntity.ok(service.find(tenantId, PageRequest.of(pageNumber, pageSize ?: 50, direction ?: Sort.DEFAULT_DIRECTION, sortBy ?: "id"), search ?: ""))
    }

    @DeleteMapping("/tenant/{tenantId}/{id}", produces = ["application/json"])
    @Operation(summary = "Delete by id and tenantId")
    open fun deleteByTenantIdAndId(@PathVariable tenantId: String, @PathVariable id: String) {
        return service.delete(tenantId, id)
    }
}
