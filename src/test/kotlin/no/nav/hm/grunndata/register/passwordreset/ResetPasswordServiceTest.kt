package no.nav.hm.grunndata.register.passwordreset

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldNotBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.aadgraph.EmailService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.util.UUID

@MicronautTest
class ResetPasswordServiceTest(
    private val supplierRegistrationService: SupplierRegistrationService,
    private val userRepository: UserRepository,
    private val otpRepository: OtpRepository,
    private val resetPasswordService: ResetPasswordService,
) {
    @MockBean(EmailService::class)
    fun mockEmailService(): EmailService = mockk(relaxed = true)

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun requestOtpTest() {
        runBlocking {
            val testSupplierRegistration =
                supplierRegistrationService.save(
                    SupplierRegistrationDTO(
                        id = UUID.randomUUID(),
                        supplierData =
                            SupplierData(
                                email = "supplier1-${UUID.randomUUID()}@test.test",
                                address = "address 1",
                                homepage = "https://www.hompage.no",
                                phone = "+47 12345678",
                            ),
                        identifier = "supplier1-${UUID.randomUUID()}",
                        name = "Supplier AS1",
                    ),
                )

            val email = "reset1-${UUID.randomUUID()}@nav.no"
            userRepository.createUser(
                User(
                    name = "First Family",
                    email = email,
                    token = "token123",
                    roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplierRegistration.id.toString())),
                ),
            )

            val userBeforeUpdate = userRepository.findByEmailIgnoreCase(email)!!

            resetPasswordService.requestOtp(email)

            val otp = otpRepository.findByEmailAndUsedOrderByCreatedDesc(email, false)!!

            resetPasswordService.verifyOtp(otp.otp, email)

            resetPasswordService.resetPassword(otp.otp, email, "newPassword123")
            val userAfterUpdate = userRepository.findByEmailIgnoreCase(email)!!

            userBeforeUpdate.token shouldNotBe userAfterUpdate.token

            shouldThrow<Exception> {
                resetPasswordService.resetPassword(otp.otp, email, "anotherPassword")
            }
        }
    }

    @Test
    fun otpLockoutAfterMaxAttemptsTest() {
        runBlocking {
            val testSupplierRegistration =
                supplierRegistrationService.save(
                    SupplierRegistrationDTO(
                        id = UUID.randomUUID(),
                        supplierData =
                            SupplierData(
                                email = "supplier2-${UUID.randomUUID()}@test.test",
                                address = "address 2",
                                homepage = "https://www.hompage.no",
                                phone = "+47 12345678",
                            ),
                        identifier = "supplier2-${UUID.randomUUID()}",
                        name = "Supplier AS2",
                    ),
                )

            val email = "lockout-${UUID.randomUUID()}@nav.no"
            userRepository.createUser(
                User(
                    name = "Lockout User",
                    email = email,
                    token = "token456",
                    roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplierRegistration.id.toString())),
                ),
            )

            resetPasswordService.requestOtp(email)
            val correctOtp = otpRepository.findByEmailAndUsedOrderByCreatedDesc(email, false)!!.otp

            repeat(ResetPasswordService.MAX_ATTEMPTS) {
                shouldThrow<IllegalArgumentException> {
                    resetPasswordService.verifyOtp("000000", email)
                }
            }

            // OTP is now burned; even the correct code no longer verifies
            shouldThrow<IllegalArgumentException> {
                resetPasswordService.verifyOtp(correctOtp, email)
            }
        }
    }
}
