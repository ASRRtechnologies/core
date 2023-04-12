/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.repository

import nl.asrr.core.generics.model.ICrudEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface ICrudRepository<T : ICrudEntity> : MongoRepository<T, String> {
    fun findOneById(id: String): T?
}
