package nl.asrr.common.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import nl.asrr.common.auth.dto.AuthResponse
import nl.asrr.common.auth.dto.LoginRequest
import nl.asrr.common.auth.model.BasicUser
import nl.asrr.common.auth.model.RefreshToken
import nl.asrr.common.auth.service.GenericAuthService
import nl.asrr.common.auth.service.GenericRefreshTokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid

@Tag(name = "Auth", description = "API for authentication")
@RequestMapping("/auth")
abstract class GenericAuthController<T : BasicUser>(
    private val authService: GenericAuthService<T>,
    private val refreshTokenService: GenericRefreshTokenService<T>
) {
    @PostMapping("/login")
    @Operation(description = "Given a login request, attempts to log in a user")
    open fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<AuthResponse> {
        return authService.login(request)
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/logout/{refreshToken}")
    @Operation(description = "Given a refreshToken, logs out the current user")
    open fun logout(@PathVariable refreshToken: String): ResponseEntity<Void> {
        return authService.logout(refreshToken)
    }

    @GetMapping("/refresh/{refreshToken}")
    @Operation(description = "Given a refresh token, refreshes access and refresh tokens for the corresponding user")
    open fun refresh(@PathVariable refreshToken: String): ResponseEntity<AuthResponse> {
        return refreshTokenService.refresh(refreshToken)
    }

    @GetMapping("/refresh/find-all")
    @Operation(description = "Lists all refresh tokens")
    open fun findAllRefreshTokens(): ResponseEntity<List<RefreshToken>> {
        return refreshTokenService.findAll()
    }
}
