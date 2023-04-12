package nl.asrr.core.auth.config

import nl.asrr.core.auth.jwt.GenericJwtTokenFilter
import nl.asrr.core.auth.model.BasicUser
import nl.asrr.core.auth.repository.IGenericUserRepository
import nl.asrr.core.exceptions.NotFoundException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
//import javax.servlet.http.HttpServletRequest
//import javax.servlet.http.HttpServletResponse

//@Configuration
//@EnableWebSecurity
//@EnableGlobalMethodSecurity(
//    securedEnabled = true,
//    jsr250Enabled = true,
//    prePostEnabled = true
//)
//abstract class GenericSecurityConfiguration<T : BasicUser>(
//    private val jwtTokenFilter: GenericJwtTokenFilter<T>,
//    private val userRepository: IGenericUserRepository<T>
//) : WebSecurityConfigurerAdapter() {
//
//    override fun configure(auth: AuthenticationManagerBuilder) {
//        auth.userDetailsService(
//            UserDetailsService { username ->
//                userRepository.findByUsername(username) ?: throw NotFoundException("Could not find user '$username'")
//            }
//        ).passwordEncoder(passwordEncoder())
//    }
//
//    @Bean
//    fun passwordEncoder(): PasswordEncoder {
//        return BCryptPasswordEncoder()
//    }
//
//    override fun configure(http: HttpSecurity) {
//        http
//            .cors().and()
//            .csrf().disable()
//            .sessionManagement()
//            .sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
//            .exceptionHandling()
//            .authenticationEntryPoint { _: HttpServletRequest?, response: HttpServletResponse, ex: AuthenticationException ->
//                response.sendError(
//                    HttpServletResponse.SC_UNAUTHORIZED,
//                    ex.message
//                )
//            }.and()
//            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
//    }
//
//    // Used by spring security if CORS is enabled
//    @Bean
//    fun corsFilter(): CorsFilter? {
//        val source = UrlBasedCorsConfigurationSource()
//        val config = CorsConfiguration()
//        config.allowCredentials = true
//        config.addAllowedOriginPattern("*")
//        config.addAllowedHeader("*")
//        config.addAllowedMethod("*")
//        source.registerCorsConfiguration("/**", config)
//        return CorsFilter(source)
//    }
//
//    @Bean
//    override fun authenticationManagerBean(): AuthenticationManager {
//        return super.authenticationManagerBean()
//    }
//}
