package nl.asrr.common.auth.jwt

import nl.asrr.common.auth.model.BasicUser
import nl.asrr.common.auth.repository.IGenericUserRepository
import nl.asrr.common.exceptions.NotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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

        filterChain.doFilter(request, response)
    }
}
