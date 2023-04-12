package nl.asrr.core.auth.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SignatureException
import io.jsonwebtoken.UnsupportedJwtException
import lombok.extern.log4j.Log4j2
import mu.KotlinLogging
import nl.asrr.core.auth.exception.InvalidJwtException
import nl.asrr.core.auth.model.BasicUser
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
@Log4j2
class JwtTokenUtil {
    private val logger = KotlinLogging.logger {}

    @Value("\${auth.jwt.secret}")
    private val secret: String? = null

    @Value("\${auth.jwt.access-expiration-ms}")
    private val expirationMs: Int = Int.MAX_VALUE

    @Value("\${auth.jwt.issuer}")
    private val issuer: String? = null

    fun generateAccessToken(user: BasicUser): Pair<String, Long> {
        val expirationDate = Date(System.currentTimeMillis() + expirationMs)
        val expirationDateTime = expirationDate.time

        return Pair(
            Jwts.builder()
                .setSubject("${user.id},${user.username}")
                .claim("roles", user.roles)
                .setIssuer(issuer ?: throw InvalidJwtException("JWT issuer cannot be null"))
                .setIssuedAt(Date())
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secret ?: throw InvalidJwtException("JWT secret cannot be null"))
                .compact(),
            expirationDateTime
        )
    }

    fun parseUsername(token: String): String {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).body.subject.split(",")[1]
    }

    fun validate(token: String): Boolean {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token)
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
}
