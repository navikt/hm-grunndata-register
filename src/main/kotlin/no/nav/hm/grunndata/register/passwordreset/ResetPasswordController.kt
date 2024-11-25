package no.nav.hm.grunndata.register.passwordreset

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.user.UserController
import org.slf4j.LoggerFactory

@Controller(ResetPasswordController.API_V1_RESET_PASSWORD)
@Tag(name = "Reset password")
class ResetPasswordController(
    private val resetPasswordService: ResetPasswordService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ResetPasswordController::class.java)
        const val API_V1_RESET_PASSWORD = "/api/v1/reset-password"
    }

    @Post("/")
    suspend fun resetPassword(
        @Body resetPasswordRequest: ResetPasswordRequest,
    ): HttpResponse<Any> {
        LOG.info("resetting password")
        resetPasswordService.resetPassword(resetPasswordRequest.otp,  resetPasswordRequest.email, resetPasswordRequest.newPassword)
        return HttpResponse.ok()
    }

    @Post("/otp")
    suspend fun requestOTP(
        @Body otpRequest: OTPRequest,
    ): HttpResponse<Any> {
        LOG.info("requesting otp")
        resetPasswordService.requestOtp(otpRequest.email)
        return HttpResponse.ok()
    }

    @Post("/otp/verify")
    suspend fun verifyOTP(
        @Body verifyOTPRequest: VerifyOTPRequest,
    ): HttpResponse<Any> {
        LOG.info("verifying otp")
        resetPasswordService.verifyOtp(verifyOTPRequest.otp, verifyOTPRequest.email)
        return HttpResponse.ok()
    }
}
