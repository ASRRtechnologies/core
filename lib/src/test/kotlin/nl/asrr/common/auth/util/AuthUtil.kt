package nl.asrr.common.auth.util

import nl.asrr.common.auth.model.BasicUser
import nl.asrr.common.auth.model.RefreshToken
import java.time.LocalDateTime

class AuthUtil {
    companion object {
        fun createUser(
            username: String = "user@user.nl",
            password: String = "password",
            fullName: String = "fullName",
            roles: MutableSet<String> = mutableSetOf()
        ): BasicUser {
            return BasicUser(
                "1234",
                username,
                password,
                fullName,
                LocalDateTime.now(),
                LocalDateTime.now(),
                roles = roles,
            )
        }

        fun createSuperAdmin(): BasicUser {
            return BasicUser(
                "1234",
                "admin@admin.nl",
                "password",
                "fullName",
                LocalDateTime.now(),
                LocalDateTime.now(),
                roles = mutableSetOf("SUPER_ADMIN")
            )
        }

        fun createRefreshToken(
            username: String = "username",
            token: String = "token",
            expires: LocalDateTime = LocalDateTime.now()
        ): RefreshToken {
            return RefreshToken("1234", username, token, expires)
        }
    }
}
