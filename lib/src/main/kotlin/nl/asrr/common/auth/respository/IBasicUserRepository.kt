package nl.asrr.common.auth.respository

import nl.asrr.common.auth.model.BasicUser
import org.springframework.stereotype.Repository

/**
 * Repository for [BasicUser]s
 */
@Repository
interface IBasicUserRepository : IGenericUserRepository<BasicUser> {
    override fun findByEmail(email: String): BasicUser?
    override fun deleteByEmail(email: String)
    fun existsByEmail(email: String): Boolean
}
