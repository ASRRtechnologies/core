/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.controller

import io.swagger.v3.oas.annotations.Operation
import nl.asrr.core.generics.model.ITenantCrudEntity
import nl.asrr.core.generics.service.ITenantCrudService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

abstract class ITenantCrudController<T : ITenantCrudEntity>(
    override val service: ITenantCrudService<T>
) : ICrudController<T>(service) {

    @GetMapping("/find-all/{tenantId}")
    @Operation(summary = "Find all by tenantId")
    @PreAuthorize("hasAuthority('SUPER_ADMIN') or @Security.isTenantAdminOf(#tenantId)")
    open fun findAllByTenantId(@PathVariable tenantId: String): ResponseEntity<List<ITenantCrudEntity>> {
        return ResponseEntity.ok(service.findAllByTenantId(tenantId))
    }

    @GetMapping("/tenant/find/{tenantId}/{id}")
    @Operation(summary = "Find by id and tenantId")
    @PreAuthorize("hasAuthority('SUPER_ADMIN') or @Security.isTenantAdminOf(#tenantId)")
    open fun findOneByIdAndTenantId(
        @PathVariable tenantId: String,
        @PathVariable id: String,
    ): ResponseEntity<ITenantCrudEntity> {
        return ResponseEntity.ok(service.findOneByIdAndTenantId(id, tenantId))
    }

    @GetMapping("/tenant/page/{tenantId}")
    @Operation(summary = "Get page by page number and size")
    @PreAuthorize("hasAuthority('SUPER_ADMIN') or @Security.isTenantAdminOf(#tenantId)")
    open fun getPage(
        @PathVariable tenantId: String,
        @RequestParam pageNumber: Int = 0,
        @RequestParam pageSize: Int? = 50
    ): ResponseEntity<Page<T>> {
        return ResponseEntity.ok(service.find(tenantId, PageRequest.of(pageNumber, pageSize ?: 50)))
    }

    @DeleteMapping("/tenant/{tenantId}/{id}")
    @Operation(summary = "Delete by id and tenantId")
    @PreAuthorize("hasAuthority('SUPER_ADMIN') or @Security.isTenantAdminOf(#tenantId)")
    open fun delete(@PathVariable tenantId: String, @PathVariable id: String) {
        return service.delete(tenantId, id)
    }
}
