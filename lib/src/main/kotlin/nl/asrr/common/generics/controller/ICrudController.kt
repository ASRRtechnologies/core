/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.common.generics.controller

import io.swagger.v3.oas.annotations.Operation
import nl.asrr.common.generics.model.ICrudEntity
import nl.asrr.common.generics.service.ICrudService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import javax.swing.SortOrder

abstract class ICrudController<T : ICrudEntity>(open val service: ICrudService<T>) {

    @GetMapping
    @Operation(summary = "Find all")
    open fun findAll(): ResponseEntity<List<T>> {
        return ResponseEntity.ok(service.findAll())
    }

    @GetMapping("/find/{id}")
    @Operation(summary = "Find by id")
    open fun find(@PathVariable id: String): ResponseEntity<T> {
        return ResponseEntity.ok(service.find(id))
    }

    @GetMapping("/page")
    @Operation(summary = "Get page by page number and size with optional sorting based on field to sortBy and direction")
    open fun getPage(
        @RequestParam pageNumber: Int = 0,
        @RequestParam pageSize: Int? = 50,
        @RequestParam sortBy: String? = null,
        @RequestParam direction: Sort.Direction? = Sort.DEFAULT_DIRECTION
    ): ResponseEntity<Page<T>> {
        return ResponseEntity.ok(service.find(pageNumber, pageSize, sortBy, direction ?: Sort.DEFAULT_DIRECTION))
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete by id")
    open fun delete(@PathVariable id: String) {
        service.delete(id)
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    @Operation(summary = "Delete by list of ids")
    open fun delete(@RequestBody ids: List<String>) {
        service.delete(ids)
    }
}
