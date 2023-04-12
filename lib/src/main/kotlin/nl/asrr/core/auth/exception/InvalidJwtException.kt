package nl.asrr.core.auth.exception

/**
 * Thrown when JWT format is invalid
 */
class InvalidJwtException(override val message: String?) : Exception(message)
