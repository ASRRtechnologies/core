package nl.asrr.core.auth.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import nl.asrr.core.auth.model.BasicUser
import nl.asrr.core.auth.repository.IGenericUserRepository
import nl.asrr.core.exceptions.NotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now

abstract class GenericJwtTokenFilter<T : BasicUser>(
    private val jwtTokenUtil: JwtTokenUtil,
    private val userRepository: IGenericUserRepository<T>
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // validate auth header
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header.isNullOrEmpty() || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        // validate jwt token
        val token = header.substringAfter("Bearer ")
        if (!jwtTokenUtil.validate(token)) {
            filterChain.doFilter(request, response)
            return
        }

        // get user identity and set it on the spring security context
        val username = jwtTokenUtil.parseUsername(token)
        val user = userRepository.findByUsername(username) ?: throw NotFoundException("User '$username' does not exist")
        val auth = UsernamePasswordAuthenticationToken(user, null, user.authorities)
        auth.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = auth

        // update last login date
        user.lastLogin = now()
        userRepository.save(user)

        filterChain.doFilter(request, response)
    }
}
