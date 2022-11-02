/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.common.generics.service

import nl.asrr.common.exceptions.NotFoundException
import nl.asrr.common.generics.model.ICrudEntity
import nl.asrr.common.generics.repository.ICrudRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/**
 * Generic service for CRUD operations
 */
abstract class ICrudService<T : ICrudEntity>(open val repository: ICrudRepository<T>) {

    /**
     * Find entity by id
     * @param id The id of the entity
     */
    fun find(id: String): T {
        val found = repository.findById(id)
        if (found.isEmpty) throw NotFoundException(id)
        return found.get()
    }

    /**
     * Find all entities
     * @param page The page number
     * @param pageSize The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     */
    fun find(page: Int = 0, pageSize: Int?, sortBy: String? = null, sortDirection: Sort.Direction = Sort.DEFAULT_DIRECTION): Page<T> {
        val size = pageSize ?: 50

        val pageRequest = sortBy?.let {
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order(sortDirection, sortBy))
            )
        } ?: PageRequest.of(page, size)

        return repository.findAll(pageRequest)
    }

    /**
     * Find all entities
     */
    fun findAll(): List<T> {
        return repository.findAll()
    }

    /**
     * Delete entity by id
     * @param id The id of the entity
     */
    open fun delete(id: String) {
        repository.deleteById(id)
    }

    /**
     * Delete entities by list of ids
     * @param ids The list of ids
     */
    fun delete(ids: List<String>) {
        repository.deleteAllById(ids)
    }
}
