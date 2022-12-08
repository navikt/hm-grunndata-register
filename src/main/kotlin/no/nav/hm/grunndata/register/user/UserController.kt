package no.nav.hm.grunndata.register.user

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.register.security.Roles
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller("/api/v1/user")
class UserController(private val userRepository: UserRepository) {

    @Get("/")
    suspend fun getUserById(authentication: Authentication?) : HttpResponse<UserDTO> =
        if (authentication!=null) {
            userRepository.findById(authentication.attributes["id"] as UUID)
                ?.let {
                    HttpResponse.ok(it.toDTO())
                } ?: HttpResponse.notFound()
        }
        else HttpResponse.unauthorized()

    @Put("/")
    suspend fun updateUser(authentication: Authentication?, @Body userDTO: UserDTO): HttpResponse<UserDTO> =
        if (authentication != null) {
            userRepository.findById(authentication.attributes["id"] as UUID)
                ?.let { HttpResponse.ok(userRepository.update(it.copy(name = userDTO.name, email = userDTO.email, supplierUuid = userDTO.supplierUuid,
                    roles = userDTO.roles, attributes = userDTO.attributes)).toDTO()) } ?: HttpResponse.notFound()
        }
        else HttpResponse.unauthorized()

    @Put("/password")
    suspend fun changePassword(authentication: Authentication?, @Body changePassword: ChangePasswordDTO): HttpResponse<Any> =
        if (authentication != null) {
            val id = authentication.attributes["id"] as UUID
            userRepository.changePassword(id, changePassword.oldPassword, changePassword.newPassword)
            HttpResponse.ok()
        }
        else HttpResponse.unauthorized()
}

data class ChangePasswordDTO(val oldPassword: String, val newPassword: String)

