/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.cosmos.generics.controller

import io.swagger.v3.oas.annotations.Operation
import nl.asrr.cosmos.generics.model.ITenantCrudEntity
import nl.asrr.cosmos.generics.service.ITenantCrudService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

abstract class ITenantCrudController<T : ITenantCrudEntity>(
    override val service: ITenantCrudService<T>
) : ICrudController<T>(service) {

    @GetMapping("/find-all/{tenantId}")
    @Operation(summary = "Find all by tenantId")
    @PreAuthorize("hasAuthority('SUPER_ADMIN') or @Security.isTenantAdminOf(#tenantId)")
    open fun findAllByTenantId(@PathVariable tenantId: String): ResponseEntity<List<ITenantCrudEntity>> {
        return ResponseEntity.ok(service.findAllByTenantId(tenantId))
    }
}
