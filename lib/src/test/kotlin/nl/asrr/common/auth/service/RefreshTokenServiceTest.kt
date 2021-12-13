package nl.asrr.common.auth.service

import io.mockk.every
import io.mockk.mockk
import nl.asrr.common.auth.exception.ExpiredRefreshTokenException
import nl.asrr.common.auth.jwt.JwtTokenUtil
import nl.asrr.common.auth.model.RefreshToken
import nl.asrr.common.auth.respository.IBasicUserRepository
import nl.asrr.common.auth.respository.IRefreshTokenRepository
import nl.asrr.common.auth.util.AuthUtil
import nl.asrr.common.exceptions.NotFoundException
import nl.asrr.common.id.IdGenerator
import org.junit.Assert.assertThrows
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RefreshTokenServiceTest {

    @Test
    fun `generateRefreshToken returns refresh token for given user`() {
        val user = AuthUtil.createUser()
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.save(any()) } returns RefreshToken(
            "123",
            user.email,
            "token",
            LocalDateTime.now()
        )
        val refreshTokenService = createService(refreshTokenRepository = refreshTokenRepository)

        val refreshToken = refreshTokenService.generateRefreshToken(user)
        assertEquals(user.email, refreshToken.email)
    }

    @Test
    fun `refresh throws exception when token does not exist`() {
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken(any()) } returns null
        val refreshTokenService = createService(refreshTokenRepository = refreshTokenRepository)

        assertThrows(NotFoundException::class.java) {
            refreshTokenService.refresh("token")
        }
    }

    @Test
    fun `refresh throws exception when token is expired`() {
        val refreshToken = RefreshToken("123", "email", "token", LocalDateTime.MIN)
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken(any()) } returns refreshToken
        val refreshTokenService = createService(refreshTokenRepository = refreshTokenRepository)

        assertThrows(ExpiredRefreshTokenException::class.java) {
            refreshTokenService.refresh("token")
        }
    }

    @Test
    fun `refresh returns auth response when given valid token`() {
        val refreshToken = RefreshToken("123", "email", "token", LocalDateTime.MAX)
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken(any()) } returns refreshToken
        every { refreshTokenRepository.save(any()) } returns refreshToken
        every { refreshTokenRepository.delete(any()) } returns Unit

        val user = AuthUtil.createUser()
        val userRepository = mockk<IBasicUserRepository>()
        every { userRepository.findByEmail(any()) } returns user

        val refreshTokenService =
            createService(refreshTokenRepository = refreshTokenRepository, userRepository = userRepository)
        val response = refreshTokenService.refresh("token").body

        assertEquals(user.email, response.email)
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
