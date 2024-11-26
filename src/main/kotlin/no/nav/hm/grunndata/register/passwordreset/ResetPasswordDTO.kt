package no.nav.hm.grunndata.register.passwordreset

data class OTPRequest(
    val email: String,
)

data class VerifyOTPRequest(
    val otp: String,
    val email: String,
)

data class ResetPasswordRequest(
    val otp: String,
    val email: String,
    val newPassword: String,
)
