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
    private val expirationHrs: Long,
    @Value("\${auth.jwt.refresh-rotation-grace-seconds:60}")
    private val rotationGraceSeconds: Long = 60
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

        // Idempotent retry within the rotation grace window: a client that lost
        // the rotation response (e.g. mobile app killed mid-refresh) can present
        // the old token again and receives the same replacement instead of being
        // logged out. The old token's shortened expiry bounds the window.
        refreshToken.replacedByToken?.let { replacement ->
            val replacementToken = refreshTokenRepository.findByToken(replacement)
                ?: throw ExpiredRefreshTokenException("Refresh token '$token' has been consumed, please login again")
            if (isExpired(replacementToken))
                throw ExpiredRefreshTokenException("Refresh token '$token' has expired, please login again")
            val (accessToken, accessExpires) = jwtTokenUtil.generateAccessToken(user)
            return ResponseEntity(
                AuthResponse(
                    idGenerator.generate(),
                    user.username,
                    accessToken,
                    replacementToken.token,
                    accessExpires
                ),
                HttpStatus.OK
            )
        }

        val (newAccessToken, accessExpires) = jwtTokenUtil.generateAccessToken(user)
        val newRefreshToken = generateRefreshToken(user)
        val now = LocalDateTime.now(ZoneId.of("Europe/Amsterdam"))
        // Rotate with a grace window instead of a hard delete: keep the old token
        // briefly, pointing at its replacement, so a lost response is recoverable.
        refreshTokenRepository.save(
            refreshToken.copy(
                expires = now.plusSeconds(rotationGraceSeconds),
                replacedByToken = newRefreshToken.token
            )
        )
        // Rotation no longer deletes the old token, so sweep this user's expired
        // ones here to keep the collection from accumulating
        refreshTokenRepository.deleteAllByUsernameAndExpiresBefore(user.username, now)

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
