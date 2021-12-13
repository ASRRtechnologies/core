package nl.asrr.common.auth.util

import nl.asrr.common.auth.model.BasicUser
import nl.asrr.common.auth.model.RefreshToken
import nl.asrr.common.auth.model.Role
import java.time.LocalDateTime

class AuthUtil {
    companion object {
        fun createUser(
            email: String = "user@user.nl",
            password: String = "password",
            fullName: String = "fullName",
            roles: MutableSet<String> = mutableSetOf()
        ): BasicUser {
            return BasicUser(
                "1234",
                email,
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
                roles = mutableSetOf(Role.SUPER_ADMIN.name)
            )
        }

        fun createRefreshToken(
            email: String = "email",
            token: String = "token",
            expires: LocalDateTime = LocalDateTime.now()
        ): RefreshToken {
            return RefreshToken("1234", email, token, expires)
        }
    }
}
