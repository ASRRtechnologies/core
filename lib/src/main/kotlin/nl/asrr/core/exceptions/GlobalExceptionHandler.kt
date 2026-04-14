package nl.asrr.core.exceptions

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.asrr.core.auth.exception.ExpiredRefreshTokenException
import nl.asrr.core.auth.exception.InvalidJwtException
import nl.asrr.core.auth.exception.UnexpectedUserException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Default RFC 7807 (application/problem+json) handler for the standard core exceptions.
 * Apps that need different behavior can declare their own @ControllerAdvice with a higher precedence.
 */
@ControllerAdvice
@ResponseBody
open class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "not-found", ex.message ?: "Resource not found")

    @ExceptionHandler(DuplicateException::class)
    fun handleDuplicate(ex: DuplicateException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "duplicate", ex.message ?: "Resource already exists")

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(ex: ValidationException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "validation", ex.message ?: "Validation failed")

    @ExceptionHandler(InvalidOperationException::class)
    fun handleInvalidOperation(ex: InvalidOperationException): ProblemDetail =
        problem(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-operation", ex.message ?: "Invalid operation")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBeanValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        return problem(HttpStatus.BAD_REQUEST, "validation", "Request validation failed").apply {
            setProperty("errors", errors)
        }
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ProblemDetail =
        problem(HttpStatus.UNAUTHORIZED, "bad-credentials", "Invalid username or password")

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ProblemDetail =
        problem(HttpStatus.FORBIDDEN, "access-denied", ex.message ?: "Access denied")

    @ExceptionHandler(InvalidJwtException::class, UnexpectedUserException::class)
    fun handleInvalidJwt(ex: Exception): ProblemDetail =
        problem(HttpStatus.UNAUTHORIZED, "invalid-token", ex.message ?: "Invalid token")

    @ExceptionHandler(ExpiredRefreshTokenException::class)
    fun handleExpiredRefresh(ex: ExpiredRefreshTokenException): ProblemDetail =
        problem(HttpStatus.UNAUTHORIZED, "expired-refresh-token", ex.message ?: "Refresh token expired")

    private fun problem(status: HttpStatus, type: String, detail: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.type = java.net.URI.create("https://errors.asrr.nl/$type")
            title = status.reasonPhrase
        }
}
