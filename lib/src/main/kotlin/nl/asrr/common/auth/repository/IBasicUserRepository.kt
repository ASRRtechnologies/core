package nl.asrr.common.auth.repository

import nl.asrr.common.auth.model.BasicUser
import org.springframework.stereotype.Repository

/**
 * Repository for [BasicUser]s
 */
@Repository
interface IBasicUserRepository : IGenericUserRepository<BasicUser> {
    override fun findByUsername(username: String): BasicUser?
    override fun deleteByUsername(username: String)
    fun existsByUsername(email: String): Boolean
}
