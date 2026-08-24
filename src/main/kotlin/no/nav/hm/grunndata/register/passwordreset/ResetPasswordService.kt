package no.nav.hm.grunndata.register.passwordreset

import com.microsoft.graph.models.BodyType
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.aadgraph.EmailService
import no.nav.hm.grunndata.register.user.UserRepository
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.UUID

@Singleton
open class ResetPasswordService(
    private val userRepository: UserRepository,
    private val otpRepository: OtpRepository,
    private val emailService: EmailService,
) {
    companion object {
        const val MAX_ATTEMPTS = 5
        const val OTP_TTL_MINUTES = 30L
    }

    @Transactional
    open suspend fun requestOtp(email: String) {
        userRepository.findByEmailIgnoreCase(email)?.let {
            val existingOtp = otpRepository.findByEmailAndUsedOrderByCreatedDesc(email, false)
            if (existingOtp != null && existingOtp.created.plusMinutes(OTP_TTL_MINUTES).isAfter(LocalDateTime.now())) {
                return
            }
            // create and send OTP
            val otp = generateOTP()
            otpRepository.save(Otp(id = UUID.randomUUID(), email = email, otp = otp, used = false))
            emailService.sendSimpleMessage(
                to = email,
                subject = "Engangskode - Finn Hjelpemiddel - Leverandør",
                BodyType.Text,
                content = "Din engangskode er: $otp",
            )
        }
    }

    @Transactional
    open suspend fun verifyOtp(
        submittedOtp: String,
        email: String,
    ) {
        val active =
            otpRepository.findByEmailAndUsedOrderByCreatedDesc(email, false)
                ?: throw IllegalArgumentException("Invalid OTP")
        if (active.created.plusMinutes(OTP_TTL_MINUTES).isBefore(LocalDateTime.now())) {
            throw IllegalArgumentException("OTP expired")
        }
        if (active.otp != submittedOtp) {
            val attempts = active.attempts + 1
            otpRepository.update(
                active.copy(attempts = attempts, used = attempts >= MAX_ATTEMPTS, updated = LocalDateTime.now()),
            )
            throw IllegalArgumentException("Invalid OTP")
        }
    }

    @Transactional
    open suspend fun resetPassword(
        otp: String,
        email: String,
        newPassword: String,
    ) {
        val otpFromDb = otpRepository.findByOtpAndEmail(otp, email) ?: throw IllegalArgumentException("Invalid OTP" )
        if (otpFromDb.used) throw IllegalArgumentException("OTP already used")
        if (otpFromDb.created.plusMinutes(OTP_TTL_MINUTES).isBefore(LocalDateTime.now())) throw IllegalArgumentException("OTP expired")
        userRepository.findByEmailIgnoreCase(email)?.let {
            userRepository.updatePassword(it.id, newPassword)
        }
        otpRepository.update(otpFromDb.copy(used = true))
    }
}

private val secureRandom = SecureRandom()

fun generateOTP(): String = (100_000 + secureRandom.nextInt(900_000)).toString()
