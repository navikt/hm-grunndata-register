package no.nav.hm.grunndata.register.user

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import no.nav.hm.grunndata.register.api.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.hm.grunndata.register.user.UserAttribute.SUPPLIER_ID
import kotlin.collections.List

@Secured(Roles.ROLE_SUPPLIER)
@Controller(UserController.API_V1_USER_REGISTRATIONS)
class UserController(private val userRepository: UserRepository) {

    companion object {
        private val LOG = LoggerFactory.getLogger(UserController::class.java)
        const val API_V1_USER_REGISTRATIONS = "/vendor/api/v1/users"
    }
    @Get("/")
    suspend fun getUsers(authentication: Authentication?): HttpResponse<List<UserDTO>> =
        if (authentication!=null) {
            userRepository.getUsersBySupplierId(authentication.attributes[SUPPLIER_ID].toString())
                .map { it.toDTO() }
                .let {
                    HttpResponse.ok(it)
                } ?: HttpResponse.notFound()
        }
        else HttpResponse.unauthorized()

    @Get("/{userId}")
    suspend fun getUserId(userId: UUID, authentication: Authentication?): HttpResponse<UserDTO> =
        if (authentication != null) {
            userRepository.findById(userId)
                ?.takeIf { it.attributes[SUPPLIER_ID] == authentication.attributes[SUPPLIER_ID] }
                ?.let {
                    HttpResponse.ok(it.toDTO())
                } ?: HttpResponse.notFound()
        } else HttpResponse.unauthorized()

    @Put("/")
    suspend fun updateUser(authentication: Authentication?, @Body userDTO: UserDTO): HttpResponse<UserDTO> =
        if (authentication != null) {
            userRepository.findByEmail(authentication.name)
                ?.let { HttpResponse.ok(userRepository.update(it.copy(name = userDTO.name, email = userDTO.email,
                    roles = userDTO.roles, attributes = userDTO.attributes)).toDTO()) } ?: HttpResponse.notFound()
        }
        else HttpResponse.unauthorized()

    @Put("/password")
    suspend fun changePassword(authentication: Authentication?, @Body changePassword: ChangePasswordDTO): HttpResponse<Any> =
        if (authentication != null) {
            userRepository.loginUser(authentication.name, changePassword.oldPassword)?.let {
                userRepository.changePassword(it.id, changePassword.oldPassword, changePassword.newPassword)
                HttpResponse.ok()
            } ?: throw BadRequestException("Wrong user info, please check password and email is correct")
        }
        else HttpResponse.unauthorized()
}

data class ChangePasswordDTO(val oldPassword: String, val newPassword: String)

