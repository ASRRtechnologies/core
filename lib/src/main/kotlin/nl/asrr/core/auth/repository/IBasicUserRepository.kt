package nl.asrr.core.auth.repository

import nl.asrr.core.auth.model.BasicUser
import org.springframework.stereotype.Repository

/**
 * Repository for [BasicUser]s
 */
@Repository
interface IBasicUserRepository : IGenericUserRepository<BasicUser> {
    override fun findByUsername(username: String): BasicUser?
    override fun deleteByUsername(username: String)
    fun existsByUsername(username: String): Boolean
}
