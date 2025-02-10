package no.nav.hm.grunndata.register.user

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.UserAttribute.USER_ID
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_HMS)
@Controller(UserHmsController.API_V1_USER_REGISTRATIONS)
@Tag(name="HMS User")
class UserHmsController(private val userRepository: UserRepository) {

    companion object {
        private val LOG = LoggerFactory.getLogger(UserController::class.java)
        const val API_V1_USER_REGISTRATIONS = "/hms-user/api/v1/users"
    }

    @Get("/{userId}")
    suspend fun getUserId(userId: UUID, authentication: Authentication): HttpResponse<UserDTO> =
        userRepository.findById(authentication.attributes[USER_ID] as UUID)
            ?.let {
                HttpResponse.ok(it.toDTO())
            } ?: HttpResponse.notFound()

}

