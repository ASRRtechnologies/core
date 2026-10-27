package nl.asrr.core.exceptions

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.asrr.core.auth.exception.ExpiredRefreshTokenException
import nl.asrr.core.auth.exception.InvalidJwtException
import nl.asrr.core.auth.exception.UnexpectedUserException
import nl.asrr.core.logging.isClientDisconnect
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.UUID

/**
 * Default RFC 7807 (application/problem+json) handler for the standard core exceptions.
 * Apps that need different behavior can declare their own @ControllerAdvice with a higher precedence.
 *
 * A 500 is reserved for real faults: it says "our fault, look at the logs", so a refusal, a bad
 * request or a stale write must never end up there. Spring MVC's own exceptions (unsupported
 * method, missing parameter, unreadable body, no such route) are answered by
 * [ResponseEntityExceptionHandler] with their proper 4xx. What is left falls through to
 * [handleUnexpected], which logs once at ERROR with a request id that is returned to the caller,
 * so a screenshot of the error leads straight to the log line.
 *
 * Set `asrr.errors.explicit=true` (dev and test only) to put the exception message in that answer.
 */
@ControllerAdvice
@ResponseBody
open class GlobalExceptionHandler(
    @param:Value("\${asrr.errors.explicit:false}") private val explicitErrors: Boolean = false,
) : ResponseEntityExceptionHandler() {
    private val log = KotlinLogging.logger {}

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

    @ExceptionHandler(OperationFailedException::class)
    fun handleOperationFailed(ex: OperationFailedException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "operation-failed", ex.message ?: "Operation failed in current state")

    /** Two people saved the same record; the second one has to reload, nothing is broken. */
    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLocking(ex: OptimisticLockingFailureException): ProblemDetail =
        problem(HttpStatus.CONFLICT, "concurrent-modification", "The record was changed by someone else; reload and try again")

    /** `require(...)` in a service is a rejected input, not a crash. */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "bad-request", ex.message ?: "Invalid request")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "bad-request", "Invalid value for parameter '${ex.name}'")

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") }
        val body = problem(HttpStatus.BAD_REQUEST, "validation", "Request validation failed").apply {
            setProperty("errors", errors)
        }
        return ResponseEntity.badRequest().body(body)
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

    /**
     * Anything else is a real fault. A client that went away mid-response (closed tab, dropped
     * download) is not: there is nobody left to answer and nothing to fix, so it is logged at DEBUG
     * and no body is written.
     *
     * The request id is ours end to end: returned to the client, written to the log line, and on
     * the MDC so a Sentry logback appender attaches it to the event. Do not use the return value
     * of `Sentry.captureException()` for this: the appender already captures the throwable on the
     * log line, a second capture of the same instance is dropped as a duplicate and returns the
     * all-zero id, which is what Alpha's users were shown as "Request ID" for a while.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ProblemDetail>? {
        if (isClientDisconnect(ex)) {
            log.debug { "Client disconnected: ${ex.message}" }
            return null
        }
        val requestId = UUID.randomUUID().toString()
        MDC.put(MDC_REQUEST_ID, requestId)
        try {
            log.error(ex) { "Unhandled exception [requestId=$requestId]: ${ex.message}" }
        } finally {
            MDC.remove(MDC_REQUEST_ID)
        }
        val detail = if (explicitErrors) {
            "${ex.message ?: ex.javaClass.simpleName} (Request ID: $requestId)"
        } else {
            "An unexpected error occurred. Request ID: $requestId"
        }
        val body = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal", detail).apply { setProperty("requestId", requestId) }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }

    private fun problem(status: HttpStatus, type: String, detail: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.type = java.net.URI.create("https://errors.asrr.nl/$type")
            title = status.reasonPhrase
        }

    companion object {
        const val MDC_REQUEST_ID = "requestId"
    }
}
