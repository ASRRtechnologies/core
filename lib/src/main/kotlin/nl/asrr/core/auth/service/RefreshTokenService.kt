package nl.asrr.core.auth.service

import nl.asrr.core.auth.jwt.JwtTokenUtil
import nl.asrr.core.auth.model.BasicUser
import nl.asrr.core.auth.repository.IGenericUserRepository
import nl.asrr.core.auth.repository.IRefreshTokenRepository
import nl.asrr.core.id.IdGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class RefreshTokenService(
    refreshTokenRepository: IRefreshTokenRepository,
    userRepository: IGenericUserRepository<BasicUser>,
    jwtTokenUtil: JwtTokenUtil,
    idGenerator: IdGenerator,
    @Value("\${auth.jwt.refresh-expiration-hrs}")
    expirationHrs: Long
) : GenericRefreshTokenService<BasicUser>(
    refreshTokenRepository,
    userRepository,
    jwtTokenUtil,
    idGenerator,
    expirationHrs
)
