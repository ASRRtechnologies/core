package nl.asrr.core.auth.repository

import nl.asrr.core.auth.model.BasicUser
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * Generic repository for [BasicUser] types.
 */
@NoRepositoryBean
interface IGenericUserRepository<T : BasicUser> : MongoRepository<T, String> {
    fun findByUsername(username: String): T?
    fun deleteByUsername(username: String)
}
