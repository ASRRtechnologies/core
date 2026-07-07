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
import java.time.ZoneId
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenServiceTest {

    // The service compares token expiry against LocalDateTime.now(Europe/Amsterdam) (a deliberate
    // fixed zone, self-consistent in prod). Tests must build relative expiry timestamps in that same
    // zone, else they fail on non-Amsterdam machines (CI runs UTC → service "now" is +hours ahead).
    private val ams = ZoneId.of("Europe/Amsterdam")

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
        every { refreshTokenRepository.save(any()) } answers { firstArg() }
        every { refreshTokenRepository.deleteAllByUsernameAndExpiresBefore(any(), any()) } returns Unit

        val user = AuthUtil.createUser()
        val userRepository = mockk<IBasicUserRepository>()
        every { userRepository.findByUsername(any()) } returns user

        val refreshTokenService =
            createService(refreshTokenRepository = refreshTokenRepository, userRepository = userRepository)
        val response = refreshTokenService.refresh("token").body

        assertEquals(user.username, response!!.username)
    }

    @Test
    fun `refresh keeps rotated token usable within grace window and marks replacement`() {
        val refreshToken = RefreshToken("123", "username", "token", LocalDateTime.MAX)
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        val saved = mutableListOf<RefreshToken>()
        every { refreshTokenRepository.findByToken("token") } returns refreshToken
        every { refreshTokenRepository.save(capture(saved)) } answers { firstArg() }
        every { refreshTokenRepository.deleteAllByUsernameAndExpiresBefore(any(), any()) } returns Unit

        val user = AuthUtil.createUser()
        val userRepository = mockk<IBasicUserRepository>()
        every { userRepository.findByUsername(any()) } returns user

        val service = createService(refreshTokenRepository = refreshTokenRepository, userRepository = userRepository)
        val response = service.refresh("token").body!!

        // The old token doc is kept, points at the replacement, and only lives for the grace window
        val rotated = saved.first { it.id == "123" }
        assertEquals(response.refreshToken, rotated.replacedByToken)
        Assertions.assertTrue(rotated.expires.isBefore(LocalDateTime.now(ams).plusMinutes(5)))
    }

    @Test
    fun `refresh with rotated token inside grace returns the same replacement`() {
        val replacement = RefreshToken("456", "username", "new-token", LocalDateTime.MAX)
        val rotated = RefreshToken(
            "123", "username", "token",
            LocalDateTime.now(ams).plusSeconds(30),
            replacedByToken = "new-token"
        )
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken("token") } returns rotated
        every { refreshTokenRepository.findByToken("new-token") } returns replacement

        val user = AuthUtil.createUser()
        val userRepository = mockk<IBasicUserRepository>()
        every { userRepository.findByUsername(any()) } returns user

        val service = createService(refreshTokenRepository = refreshTokenRepository, userRepository = userRepository)
        val response = service.refresh("token").body!!

        assertEquals("new-token", response.refreshToken)
    }

    @Test
    fun `refresh with rotated token after grace throws expired`() {
        val rotated = RefreshToken(
            "123", "username", "token",
            LocalDateTime.now(ams).minusSeconds(1),
            replacedByToken = "new-token"
        )
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken("token") } returns rotated

        val service = createService(refreshTokenRepository = refreshTokenRepository)

        Assertions.assertThrows(ExpiredRefreshTokenException::class.java) {
            service.refresh("token")
        }
    }

    @Test
    fun `refresh with rotated token whose replacement is gone throws expired`() {
        val rotated = RefreshToken(
            "123", "username", "token",
            LocalDateTime.now(ams).plusSeconds(30),
            replacedByToken = "new-token"
        )
        val refreshTokenRepository = mockk<IRefreshTokenRepository>()
        every { refreshTokenRepository.findByToken("token") } returns rotated
        every { refreshTokenRepository.findByToken("new-token") } returns null

        val user = AuthUtil.createUser()
        val userRepository = mockk<IBasicUserRepository>()
        every { userRepository.findByUsername(any()) } returns user

        val service = createService(refreshTokenRepository = refreshTokenRepository, userRepository = userRepository)

        Assertions.assertThrows(ExpiredRefreshTokenException::class.java) {
            service.refresh("token")
        }
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
