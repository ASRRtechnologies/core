package nl.asrr.core.auth.service

import io.mockk.every
import io.mockk.mockk
import nl.asrr.core.auth.exception.ExpiredRefreshTokenException
import nl.asrr.core.auth.jwt.JwtTokenUtil
import nl.asrr.core.auth.model.RefreshToken
import nl.asrr.core.auth.repository.IBasicUserRepository
import nl.asrr.core.auth.repository.IRefreshTokenRepository
import nl.asrr.core.auth.util.AuthUtil
import nl.asrr.core.exceptions.NotFoundException
import nl.asrr.core.id.IdGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenServiceTest {

    @Test
    fun `generateRefreshToken returns refresh token for given user`() {
        val user = AuthUtil.createUser()
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.save(any()) } returns RefreshToken(
            "123",
            user.username,
            "token",
            LocalDateTime.now()
        )
        val refreshTokenService = createService(refreshTokenRepository = refreshTokenRepository)

        val refreshToken = refreshTokenService.generateRefreshToken(user)
        assertEquals(user.username, refreshToken.username)
    }

    @Test
    fun `refresh throws exception when token does not exist`() {
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken(any()) } returns null
        val refreshTokenService = createService(refreshTokenRepository = refreshTokenRepository)

        Assertions.assertThrows(NotFoundException::class.java) {
            refreshTokenService.refresh("token")
        }
    }

    @Test
    fun `refresh throws exception when token is expired`() {
        val refreshToken = RefreshToken("123", "username", "token", LocalDateTime.MIN)
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken(any()) } returns refreshToken
        val refreshTokenService = createService(refreshTokenRepository = refreshTokenRepository)

        Assertions.assertThrows(ExpiredRefreshTokenException::class.java) {
            refreshTokenService.refresh("token")
        }
    }

    @Test
    fun `refresh returns auth response when given valid token`() {
        val refreshToken = RefreshToken("123", "username", "token", LocalDateTime.MAX)
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken(any()) } returns refreshToken
        every { refreshTokenRepository.save(any()) } returns refreshToken
        every { refreshTokenRepository.delete(any()) } returns Unit

        val user = AuthUtil.createUser()
        val userRepository = mockk<IBasicUserRepository>()
        every { userRepository.findByUsername(any()) } returns user

        val refreshTokenService =
            createService(refreshTokenRepository = refreshTokenRepository, userRepository = userRepository)
        val response = refreshTokenService.refresh("token").body

        assertEquals(user.username, response!!.username)
    }

    private fun createService(
        refreshTokenRepository: IRefreshTokenRepository = mockk(),
        userRepository: IBasicUserRepository = mockk(),
        jwtTokenUtil: JwtTokenUtil = mockk(),
        idGenerator: IdGenerator = mockk()
    ): RefreshTokenService {
        every { idGenerator.generate() } returns "1234"
        every { jwtTokenUtil.generateAccessToken(any()) } returns Pair("accessToken", Long.MAX_VALUE)
        return RefreshTokenService(refreshTokenRepository, userRepository, jwtTokenUtil, idGenerator, 2)
    }
}
