/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.controller

import io.swagger.v3.oas.annotations.Operation
import nl.asrr.core.generics.model.ICrudEntity
import nl.asrr.core.generics.service.ICrudService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

abstract class ICrudController<T : ICrudEntity>(open val service: ICrudService<T>) {

    @GetMapping(produces = ["application/json"])
    @Operation(summary = "Find all")
    open fun findAll(): ResponseEntity<List<T>> {
        return ResponseEntity.ok(service.findAll())
    }

    @GetMapping("/find/{id}", produces = ["application/json"])
    @Operation(summary = "Find by id")
    open fun find(@PathVariable id: String): ResponseEntity<T> {
        return ResponseEntity.ok(service.find(id))
    }

    @GetMapping("/find", produces = ["application/json"])
    @Operation(summary = "Find by list of ids")
    open fun findList(@RequestParam ids: List<String>): ResponseEntity<List<T>> {
        return ResponseEntity.ok(service.findList(ids))
    }

    @GetMapping("/page", produces = ["application/json"])
    @Operation(summary = "Get page by page number and size with optional sorting based on field to sortBy and direction")
    open fun getPage(
        @RequestParam pageNumber: Int = 0,
        @RequestParam pageSize: Int? = 50,
        @RequestParam sortBy: String? = null,
        @RequestParam direction: Sort.Direction? = Sort.DEFAULT_DIRECTION,
        @RequestParam(required = false) search: String? = null,
    ): ResponseEntity<Page<T>> {
        return ResponseEntity.ok(
            service.find(
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
    @Operation(summary = "Delete by id")
    open fun deleteById(@PathVariable id: String) {
        service.delete(id)
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    @Operation(summary = "Delete by list of ids")
    open fun deleteList(@RequestBody ids: List<String>) {
        service.delete(ids)
    }
}
