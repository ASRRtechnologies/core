package nl.asrr.common.auth.exception

/**
 * Thrown when a refresh token is expired
 */
class ExpiredRefreshTokenException(override val message: String?) : Exception(message)
