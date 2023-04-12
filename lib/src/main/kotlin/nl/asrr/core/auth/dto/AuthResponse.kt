package nl.asrr.core.auth.dto

data class AuthResponse(
    val id: String,
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresUnix: Long
)
