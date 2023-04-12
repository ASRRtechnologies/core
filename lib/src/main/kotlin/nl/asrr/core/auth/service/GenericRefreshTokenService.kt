package nl.asrr.core.auth.service

import nl.asrr.core.auth.dto.AuthResponse
import nl.asrr.core.auth.exception.ExpiredRefreshTokenException
import nl.asrr.core.auth.exception.UnexpectedUserException
import nl.asrr.core.auth.jwt.JwtTokenUtil
import nl.asrr.core.auth.model.BasicUser
import nl.asrr.core.auth.model.RefreshToken
import nl.asrr.core.auth.repository.IGenericUserRepository
import nl.asrr.core.auth.repository.IRefreshTokenRepository
import nl.asrr.core.exceptions.NotFoundException
import nl.asrr.core.id.IdGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

abstract class GenericRefreshTokenService<T : BasicUser>(
    private val refreshTokenRepository: IRefreshTokenRepository,
    private val userRepository: IGenericUserRepository<T>,
    private val jwtTokenUtil: JwtTokenUtil,
    private val idGenerator: IdGenerator,
    @Value("\${auth.jwt.refresh-expiration-hrs}")
    private val expirationHrs: Long
) {
    fun generateRefreshToken(user: BasicUser): RefreshToken {
        val token = UUID.randomUUID().toString()
        val refreshToken = RefreshToken(
            idGenerator.generate(),
            user.username,
            token,
            LocalDateTime.now(ZoneId.of("Europe/Amsterdam")).plusHours(expirationHrs)
        )

        refreshTokenRepository.save(refreshToken)
        return refreshToken
    }

    fun refresh(token: String): ResponseEntity<AuthResponse> {
        val refreshToken = find(token)
        if (isExpired(refreshToken))
            throw ExpiredRefreshTokenException("Refresh token '$token' has expired, please login again")

        val user = userRepository.findByUsername(refreshToken.username)
            ?: throw NotFoundException("User '${refreshToken.username}' does not exist")
        val (newAccessToken, accessExpires) = jwtTokenUtil.generateAccessToken(user)
        val newRefreshToken = generateRefreshToken(user)
        refreshTokenRepository.delete(refreshToken)

        return ResponseEntity(
            AuthResponse(
                idGenerator.generate(),
                user.username,
                newAccessToken,
                newRefreshToken.token,
                accessExpires
            ),
            HttpStatus.OK
        )
    }

    fun findAll(): ResponseEntity<List<RefreshToken>> {
        return ResponseEntity(refreshTokenRepository.findAll(), HttpStatus.OK)
    }

    fun deleteRefreshTokenForUser(username: String, token: String) {
        val refreshToken = find(token)
        if (refreshToken.username != username)
            throw UnexpectedUserException("The current user does not match the user linked to the refresh token")
        refreshTokenRepository.delete(refreshToken)
    }

    // can be used for something like a "sign out from all devices" method
    fun deleteAllRefreshTokensForUser(username: String) {
        refreshTokenRepository.deleteAllByUsername(username)
    }

    private fun find(token: String): RefreshToken {
        return refreshTokenRepository.findByToken(token)
            ?: throw NotFoundException("Refresh token '$token' does not exist")
    }

    private fun isExpired(token: RefreshToken): Boolean {
        val now = LocalDateTime.now(ZoneId.of("Europe/Amsterdam"))
        return now.isAfter(token.expires)
    }
}
