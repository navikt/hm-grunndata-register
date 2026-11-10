package no.nav.hm.grunndata.register.security

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.AuthenticationRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class UserPasswordAuthenticationProviderTest {
    @Test
    fun `should sanitize failed login logs without changing credentials`() {
        val identity = "invalid\r\nERROR forged-event"
        val secret = "password"
        val repository = mockk<UserRepository>()
        coEvery { repository.loginUser(identity, secret) } returns null
        val provider = UserPasswordAuthenticationProvider(repository, mockk<SupplierRegistrationService>())
        val request = mockk<AuthenticationRequest<String?, String?>> {
            every { getIdentity() } returns identity
            every { getSecret() } returns secret
        }
        val logger = LoggerFactory.getLogger(UserPasswordAuthenticationProvider::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)

        try {
            provider.authenticate(null, request).isAuthenticated shouldBe false

            coVerify(exactly = 1) { repository.loginUser(identity, secret) }
            appender.list.single().formattedMessage shouldBe "User login failed invalid__ERROR forged-event"
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
