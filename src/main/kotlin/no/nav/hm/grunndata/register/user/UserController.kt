package no.nav.hm.grunndata.register.user

import io.micronaut.http.HttpAttributes
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.user.UserAttribute.SUPPLIER_ID
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.hm.grunndata.register.product.isAdmin
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.UserAdminApiController.Companion

@Secured(Roles.ROLE_SUPPLIER)
@Controller(UserController.API_V1_USER_REGISTRATIONS)
@Tag(name="Vendor User")
class UserController(private val userRepository: UserRepository,
                     private val supplierRegistrationService: SupplierRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(UserController::class.java)
        const val API_V1_USER_REGISTRATIONS = "/vendor/api/v1/users"
    }

    @Get("/")
    suspend fun getUsers(authentication: Authentication): HttpResponse<List<UserDTO>> =
        userRepository.getUsersBySupplierId(authentication.attributes[SUPPLIER_ID].toString())
            .map { it.toDTO() }
            .let {
                HttpResponse.ok(it)
            } ?: HttpResponse.notFound()

    @Get("/{userId}")
    suspend fun getUserId(userId: UUID, authentication: Authentication): HttpResponse<UserDTO> =
        userRepository.findById(userId)
            ?.takeIf { it.attributes[SUPPLIER_ID] == authentication.attributes[SUPPLIER_ID] }
            ?.let {
                HttpResponse.ok(it.toDTO())
            } ?: HttpResponse.notFound()

    @Put("/")
    suspend fun updateUser(authentication: Authentication, @Body userDTO: UserDTO): HttpResponse<UserDTO> =
        userRepository.findById(userDTO.id)
            ?.let {
                HttpResponse.ok(
                    userRepository.update(
                        it.copy(
                            name = userDTO.name,
                            email = userDTO.email,
                            attributes = mergeUserAttributes(it.attributes, userDTO.attributes)
                        )
                    ).toDTO()
                )
            } ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createUser(
        authentication: Authentication,
        @Body dto: UserRegistrationDTO,
    ): HttpResponse<UserDTO> {
        if (authentication.isAdmin()) throw BadRequestException("Only vendors can create users here")
        val supplierId = authentication.supplierId()
        LOG.info("Creating user ${dto.id} by vendor ${supplierId}")
        if (supplierRegistrationService.findById(supplierId) == null) throw BadRequestException("Unknown vendor id $supplierId")
        if (userRepository.getUsersBySupplierId(supplierId.toString()).size >= 10) throw BadRequestException("Vendor can only have 10 users")
        val entity =
            User(
                id = dto.id,
                name = dto.name,
                email = dto.email,
                token = dto.password,
                roles = listOf(Roles.ROLE_SUPPLIER),
                attributes = dto.attributes + mapOf(SUPPLIER_ID to supplierId.toString()),
            )
        userRepository.createUser(entity)
        return HttpResponse.created(entity.toDTO())
    }

    private fun mergeUserAttributes(
        inDbAttributes: Map<String, String>,
        attributes: Map<String, String>
    ): Map<String, String> {
        return inDbAttributes + attributes.filterKeys { it != SUPPLIER_ID }
    }

    @Put("/password")
    suspend fun changePassword(
        authentication: Authentication,
        @Body changePassword: ChangePasswordDTO
    ): HttpResponse<Any> =
        userRepository.loginUser(authentication.name, changePassword.oldPassword)?.let {
            userRepository.changePassword(it.id, changePassword.oldPassword, changePassword.newPassword)
            HttpResponse.ok()
        } ?: throw BadRequestException("Wrong user info, please check password and email is correct")
}

data class ChangePasswordDTO(val oldPassword: String, val newPassword: String)

