package nl.asrr.core.auth.jwt

import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import nl.asrr.core.auth.exception.InvalidJwtException
import nl.asrr.core.auth.model.BasicUser
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenUtil {
    private val logger = KotlinLogging.logger {}

    @Value("\${auth.jwt.secret}")
    private val secret: String? = null

    @Value("\${auth.jwt.access-expiration-ms}")
    private val expirationMs: Int = Int.MAX_VALUE

    @Value("\${auth.jwt.issuer}")
    private val issuer: String? = null

    private val signingKey: SecretKey by lazy {
        val raw = secret ?: throw InvalidJwtException("JWT secret cannot be null")
        // jjwt 0.12 requires HS512 keys to be at least 512 bits (64 bytes).
        val bytes = raw.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size >= 64) {
            "auth.jwt.secret must be at least 64 bytes (512 bits) for HS512"
        }
        Keys.hmacShaKeyFor(bytes)
    }

    fun generateAccessToken(user: BasicUser): Pair<String, Long> {
        val expirationDate = Date(System.currentTimeMillis() + expirationMs)

        val token = Jwts.builder()
            .subject("${user.id},${user.username}")
            .claim("roles", user.roles)
            .issuer(issuer ?: throw InvalidJwtException("JWT issuer cannot be null"))
            .issuedAt(Date())
            .expiration(expirationDate)
            .signWith(signingKey, Jwts.SIG.HS512)
            .compact()

        return token to expirationDate.time
    }

    fun parseUsername(token: String): String {
        return parseClaims(token).subject.split(",")[1]
    }

    fun validate(token: String): Boolean {
        try {
            parseClaims(token)
            return true
        } catch (ex: SignatureException) {
            logger.warn { "Invalid JWT signature - ${ex.message}" }
        } catch (ex: MalformedJwtException) {
            logger.warn { "Invalid JWT token - ${ex.message}" }
        } catch (ex: ExpiredJwtException) {
            logger.warn { "Expired JWT token - ${ex.message}" }
        } catch (ex: UnsupportedJwtException) {
            logger.warn { "Unsupported JWT token - ${ex.message}" }
        } catch (ex: IllegalArgumentException) {
            logger.warn { "JWT claims string is empty - ${ex.message}" }
        }
        return false
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
