package no.nav.hm.grunndata.register.passwordreset

import com.microsoft.graph.models.BodyType
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.aadgraph.EmailService
import no.nav.hm.grunndata.register.user.UserRepository
import java.time.LocalDateTime
import java.util.UUID

@Singleton
open class ResetPasswordService(
    private val userRepository: UserRepository,
    private val otpRepository: OtpRepository,
    private val emailService: EmailService,
) {
    @Transactional
    open suspend fun requestOtp(email: String) {
        userRepository.findByEmail(email)?.let {
            // create and send OTP
            val otp = generateOTP()
            otpRepository.save(Otp(id = UUID.randomUUID(), email = email, otp = otp, used = false))
            emailService.sendSimpleMessage(
                to = email,
                subject = "Engangskode - Finn Hjelpemiddel - Leverandør",
                BodyType.TEXT,
                content = "Din engangskode er: $otp",
            )
        }
    }

    open suspend fun verifyOtp(
        otp: String,
        email: String,
    ) {
        val otp = otpRepository.findByOtpAndEmail(otp, email) ?: throw IllegalArgumentException("Invalid OTP")
        if (otp.used) throw IllegalArgumentException("OTP already used")
        if (otp.created.plusMinutes(30).isBefore(LocalDateTime.now())) throw IllegalArgumentException("OTP expired")
    }

    @Transactional
    open suspend fun resetPassword(
        otp: String,
        email: String,
        newPassword: String,
    ) {
        val otp = otpRepository.findByOtpAndEmail(otp, email) ?: throw IllegalArgumentException("Invalid OTP")
        if (otp.used) throw IllegalArgumentException("OTP already used")
        if (otp.created.plusMinutes(30).isBefore(LocalDateTime.now())) throw IllegalArgumentException("OTP expired")
        userRepository.findByEmail(email)?.let {
            userRepository.updatePassword(it.id, newPassword)
        }
        otpRepository.update(otp.copy(used = true))
    }
}

fun generateOTP(): String {
    val randomPin = (Math.random() * 90000).toInt() + 1000
    return randomPin.toString()
}