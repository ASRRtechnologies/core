package nl.asrr.common.auth.respository

import nl.asrr.common.auth.model.BasicUser
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * Generic repository for [BasicUser] types.
 */
@NoRepositoryBean
interface IBasicUserRepository<T : BasicUser> : MongoRepository<T, String> {
    fun findByEmail(email: String): T?
    fun deleteByEmail(email: String)
}
