package nl.asrr.common.auth.service

import nl.asrr.common.auth.jwt.JwtTokenUtil
import nl.asrr.common.auth.model.BasicUser
import nl.asrr.common.auth.repository.IGenericUserRepository
import nl.asrr.common.auth.repository.IRefreshTokenRepository
import nl.asrr.common.id.IdGenerator
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
