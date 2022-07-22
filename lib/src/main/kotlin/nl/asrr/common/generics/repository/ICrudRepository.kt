/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.cosmos.generics.repository

import nl.asrr.cosmos.generics.model.ICrudEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface ICrudRepository<T : ICrudEntity> : MongoRepository<T, String> {
    fun findOneById(id: String): T?
}
