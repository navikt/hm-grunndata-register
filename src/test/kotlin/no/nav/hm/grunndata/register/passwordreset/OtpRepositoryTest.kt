package no.nav.hm.grunndata.register.passwordreset

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.UUID

@MicronautTest
class OtpRepositoryTest(
    private val otpRepository: OtpRepository,
) {
    @Test
    fun crudRepositoryTest() {
        val otp =
            Otp(
                id = UUID.randomUUID(),
                email = "test@test.nav",
                otp = "123456",
                used = false,
            )

        val otp2 =
            Otp(
                id = UUID.randomUUID(),
                email = "test@test.nav",
                otp = "123456",
                used = false,
            )

        val otp3 =
            Otp(
                id = UUID.randomUUID(),
                email = "test@test.nav",
                otp = "123456",
                used = false,
            )

        runBlocking {
            otpRepository.save(otp)
            val foundOtp = otpRepository.findByOtpAndEmail(otp.otp, "test@test.nav")
            foundOtp.shouldNotBeNull()
            otpRepository.save(otp2)
            otpRepository.save(otp3)

            val lastlyCreated = otpRepository.findByEmailAndUsedOrderByCreatedDesc("test@test.nav", false)
            lastlyCreated.shouldNotBeNull()
            lastlyCreated.id.shouldBe(otp3.id)
        }
    }
}
