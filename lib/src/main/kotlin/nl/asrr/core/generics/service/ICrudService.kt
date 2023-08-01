/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.service

import nl.asrr.core.exceptions.NotFoundException
import nl.asrr.core.generics.model.ICrudEntity
import nl.asrr.core.generics.repository.ICrudRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.TextCriteria

/**
 * Generic service for CRUD operations
 */
abstract class ICrudService<T : ICrudEntity>(open val repository: ICrudRepository<T>) {

    open fun save(entity: T): T {
        return repository.save(entity)
    }

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
     * Find entities by list of ids
     * @param ids The list of ids
     */
    fun findList(ids: List<String>): List<T> {
        return repository.findAllById(ids).toList()
    }

    /**
     * Find all entities
     * @param page The page number
     * @param pageSize The page size
     * @param sortBy The field to sort by
     * @param sortDirection The sort direction
     */
    fun find(pageable: Pageable, search: String): Page<T> {
        if (search.isBlank()) {
            return repository.findAll(pageable)
        }

        val criteria = TextCriteria().matchingAny(search)

        return repository.findAllBy(criteria, pageable)
    }

    /**
     * Find all entities
     */
    fun findAll(): List<T> {
        return repository.findAll()
    }

    /**
     * Check if entity exists
     * @param id The id of the entity
     * @return True if entity exists, false otherwise
     */
    fun exists(id: String): Boolean {
        return repository.existsById(id)
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
