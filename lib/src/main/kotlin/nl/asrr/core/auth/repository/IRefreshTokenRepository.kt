package nl.asrr.core.auth.repository

import nl.asrr.core.auth.model.RefreshToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface IRefreshTokenRepository : MongoRepository<RefreshToken, String> {
    fun findByToken(token: String): RefreshToken?
    fun deleteAllByUsernameIgnoreCase(username: String)
    fun findAllByUsernameIgnoreCase(username: String): List<RefreshToken>
    fun deleteAllByUsernameIgnoreCaseAndExpiresBefore(username: String, expires: LocalDateTime)
}
