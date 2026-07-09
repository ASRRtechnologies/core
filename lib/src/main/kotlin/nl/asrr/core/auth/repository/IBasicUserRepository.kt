package nl.asrr.core.auth.repository

import nl.asrr.core.auth.model.BasicUser
import org.springframework.stereotype.Repository

/**
 * Repository for [BasicUser]s
 */
@Repository
interface IBasicUserRepository : IGenericUserRepository<BasicUser> {
    override fun findByUsernameIgnoreCase(username: String): BasicUser?
    override fun deleteByUsernameIgnoreCase(username: String)
    fun existsByUsernameIgnoreCase(username: String): Boolean
}
