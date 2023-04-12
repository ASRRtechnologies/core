package nl.asrr.core.auth.repository

import nl.asrr.core.auth.model.RefreshToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface IRefreshTokenRepository : MongoRepository<RefreshToken, String> {
    fun findByToken(token: String): RefreshToken?
    fun deleteAllByUsername(username: String)
    fun findAllByUsername(username: String): List<RefreshToken>
}
