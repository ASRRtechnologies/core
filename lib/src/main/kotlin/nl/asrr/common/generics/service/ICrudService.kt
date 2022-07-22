/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.cosmos.generics.service

import nl.asrr.common.exceptions.NotFoundException
import nl.asrr.cosmos.generics.model.ICrudEntity
import nl.asrr.cosmos.generics.repository.ICrudRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest

abstract class ICrudService<T : ICrudEntity>(open val repository: ICrudRepository<T>) {

    fun find(id: String): T {
        val found = repository.findById(id)
        if (found.isEmpty) throw NotFoundException(id)
        return found.get()
    }

    fun find(pageNumber: Int = 0, pageSize: Int?): Page<T> {
        return repository.findAll(PageRequest.of(pageNumber, pageSize ?: 50))
    }

    fun delete(id: String) {
        repository.deleteById(id)
    }
}
