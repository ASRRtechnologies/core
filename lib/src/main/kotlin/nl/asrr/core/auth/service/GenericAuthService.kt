package nl.asrr.core.auth.service

import nl.asrr.core.auth.dto.AuthResponse
import nl.asrr.core.auth.dto.LoginRequest
import nl.asrr.core.auth.exception.UnexpectedUserException
import nl.asrr.core.auth.jwt.JwtTokenUtil
import nl.asrr.core.auth.model.BasicUser
import nl.asrr.core.exceptions.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

abstract class GenericAuthService<T : BasicUser>(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenUtil: JwtTokenUtil,
    private val refreshTokenService: GenericRefreshTokenService<T>
) {
    @Suppress("UNCHECKED_CAST")
    fun login(request: LoginRequest): ResponseEntity<AuthResponse> {
        return try {
            val authentication =
                authenticationManager.authenticate(UsernamePasswordAuthenticationToken(request.username, request.password))

            val user = authentication.principal as T

            val (accessToken, accessExpires) = jwtTokenUtil.generateAccessToken(user)
            val refreshToken = refreshTokenService.generateRefreshToken(user)

            ResponseEntity.ok(
                AuthResponse(
                    user.id,
                    user.username,
                    accessToken,
                    refreshToken.token,
                    accessExpires
                )
            )
        } catch (ex: BadCredentialsException) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun logout(refreshToken: String): ResponseEntity<Void> {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = authentication.principal as T
        try {
            refreshTokenService.deleteRefreshTokenForUser(user.username, refreshToken)
        } catch (ex: NotFoundException) {
            return ResponseEntity<Void>(HttpStatus.NO_CONTENT)
        } catch (ex: UnexpectedUserException) {
            return ResponseEntity<Void>(HttpStatus.UNAUTHORIZED)
        }
        return ResponseEntity<Void>(HttpStatus.NO_CONTENT)
    }
}
