package nl.asrr.common.auth.model

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime

/**
 * Simplest form of a user with only basic (required) properties.
 * Can be extended to create more extensive users.
 */
open class BasicUser(
    @Id
    val id: String,

    @Indexed(unique = true)
    private val username: String,

    private var password: String,

    var fullName: String,

    /**
     * Date that the user is created in Europe/Amsterdam time format
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val created: LocalDateTime,

    /**
     * Date that the user is updated in Europe/Amsterdam time format
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    var updated: LocalDateTime,

    /**
     * Indicates whether the user is enabled or disabled. A disabled user cannot be authenticated
     */
    private var enabled: Boolean = true,

    /**
     * The roles of the user
     */
    var roles: MutableSet<String> = mutableSetOf()

) : UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        val authorities = mutableListOf<SimpleGrantedAuthority>()

        roles.forEach { authorities.add(SimpleGrantedAuthority(it)) }

        return authorities
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonExpired(): Boolean {
        return enabled
    }

    override fun isAccountNonLocked(): Boolean {
        return enabled
    }

    override fun isCredentialsNonExpired(): Boolean {
        return enabled
    }

    override fun isEnabled(): Boolean {
        return enabled
    }

    fun setPassword(newPassword: String) {
        this.password = newPassword
    }
}
