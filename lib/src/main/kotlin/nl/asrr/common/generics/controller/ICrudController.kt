/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.cosmos.generics.controller

import io.swagger.v3.oas.annotations.Operation
import nl.asrr.cosmos.generics.model.ICrudEntity
import nl.asrr.cosmos.generics.service.ICrudService
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus

abstract class ICrudController<T : ICrudEntity>(open val service: ICrudService<T>) {

    @GetMapping("/find/{id}")
    @Operation(summary = "Find by id")
    open fun find(@PathVariable id: String): ResponseEntity<ICrudEntity> {
        return ResponseEntity.ok(service.find(id))
    }

    @GetMapping("/page")
    @Operation(summary = "Get page by page number and size")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    open fun getPage(@RequestParam pageNumber: Int, @RequestParam pageSize: Int? = 50): ResponseEntity<Page<T>> {
        return ResponseEntity.ok(service.find(pageNumber, pageSize))
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete by id")
    open fun delete(@PathVariable id: String) {
        service.delete(id)
    }
}
