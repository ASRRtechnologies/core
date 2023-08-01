/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.repository

import nl.asrr.core.generics.model.ICrudEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.repository.MongoRepository

interface ICrudRepository<T : ICrudEntity> : MongoRepository<T, String> {
    fun findOneById(id: String): T?
    fun findAllBy(criteria: TextCriteria, pageable: Pageable): Page<T>
}
