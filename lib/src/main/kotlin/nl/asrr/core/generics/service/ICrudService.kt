/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.service

import nl.asrr.core.exceptions.NotFoundException
import nl.asrr.core.generics.model.ICrudEntity
import nl.asrr.core.generics.model.IEntitySearch
import nl.asrr.core.generics.repository.ICrudRepository
import org.springframework.beans.support.PagedListHolder
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.core.query.UntypedExampleMatcher

/**
 * Generic service for CRUD operations
 */
abstract class ICrudService<T : ICrudEntity>(
    open val repository: ICrudRepository<T>,
    open val mongoTemplate: MongoTemplate
) {

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

    inline fun <reified T> search(
        search: IEntitySearch,
        pageNumber: Int,
        pageSize: Int
    ): Page<T> {
        val matcher = UntypedExampleMatcher.matchingAny()
            .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
            .withIgnoreCase()
            .withIgnoreNullValues()

        val example = Example.of(search, matcher)
        val query = Query(Criteria().alike(example))
        val totalItems = mongoTemplate.find(query, T::class.java)

        val page = PagedListHolder(totalItems)
        page.page = pageNumber
        page.pageSize = pageSize

        val pageable = PageRequest.of(pageNumber, pageSize)
        return PageImpl(page.pageList, pageable, totalItems.size.toLong())
    }

    inline fun <reified T> search(
        search: IEntitySearch,
        pageNumber: Int,
        pageSize: Int,
        sortBy: String,
        direction: Sort.Direction
    ): Page<T> {
        val matcher = UntypedExampleMatcher.matchingAny()
            .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
            .withIgnoreCase()
            .withIgnoreNullValues()

        val example = Example.of(search, matcher)
        val sort = Sort.by(direction, sortBy)
        val query = Query(Criteria().alike(example)).with(sort)
        val totalItems = mongoTemplate.find(query, T::class.java)

        val page = PagedListHolder(totalItems)
        page.page = pageNumber
        page.pageSize = pageSize

        val pageable = PageRequest.of(pageNumber, pageSize, sort)
        return PageImpl(page.pageList, pageable, totalItems.size.toLong())
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
    fun exists(id: String, strict: Boolean = false): Boolean {
        val exists =  repository.existsById(id)
        if (strict && !exists) throw NotFoundException(id)
        return exists
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
