package no.nav.hm.grunndata.register.user

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.UserAdminApiController.Companion.API_V1_ADMIN_USER_REGISTRATIONS
import no.nav.hm.grunndata.register.user.UserAttribute.SUPPLIER_ID
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.hm.grunndata.register.runtime.where

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_USER_REGISTRATIONS)
@Tag(name="Admin User")
class UserAdminApiController(
    private val userRepository: UserRepository,
    private val supplierRegistrationService: SupplierRegistrationService,
) {
    companion object {
        const val API_V1_ADMIN_USER_REGISTRATIONS = "/admin/api/v1/users"
        private val LOG = LoggerFactory.getLogger(UserAdminApiController::class.java)
    }

    @Get("/")
    suspend fun getUsers(
        @RequestBean criteria: UserAdminCriteria,
        pageable: Pageable,
    ): Page<UserDTO> = userRepository.findAll(buildCriteriaSpec(criteria), pageable).map { it.toDTO() }

    private fun buildCriteriaSpec(criteria: UserAdminCriteria): PredicateSpecification<User>? =
        if (criteria.isNotEmpty()) {
            where {
                criteria.emailLower()?.let { root[User::email] eq it.lowercase() }
                criteria.name?.let { root[User::name] like LiteralExpression("%$it%") }
            }
        } else null

    @Get("/supplierId/{supplierId}")
    suspend fun getUsersBySupplierId(supplierId: UUID): List<UserDTO> =
        userRepository.getUsersBySupplierId(supplierId.toString()).map { it.toDTO() }

    @Post("/")
    suspend fun createUser(
        @Body dto: UserRegistrationDTO,
    ): HttpResponse<UserDTO> {
        LOG.info("Creating user ${dto.id} ")
        if (dto.roles.isEmpty()) throw BadRequestException("User does not have any role")
        if (dto.roles.contains(Roles.ROLE_SUPPLIER)) {
            if (!dto.attributes.containsKey(SUPPLIER_ID)) throw BadRequestException("User must be connected to a supplierId")
            val supplierId = UUID.fromString(dto.attributes[SUPPLIER_ID])
            supplierRegistrationService.findById(supplierId)
                ?: throw BadRequestException("Unknown supplier id $supplierId")
        }
        val entity =
            User(
                id = dto.id,
                name = dto.name,
                email = dto.email,
                token = dto.password,
                roles = dto.roles,
                attributes = dto.attributes,
            )
        userRepository.createUser(entity)
        return HttpResponse.created(entity.toDTO())
    }

    @Get("/{id}")
    suspend fun getUser(id: UUID): HttpResponse<UserDTO> =
        userRepository.findById(id)
            ?.let {
                HttpResponse.ok(it.toDTO())
            } ?: HttpResponse.notFound()

    @Get("/email/{email}")
    suspend fun getUserByEmail(email: String): HttpResponse<UserDTO> =
        userRepository.findByEmailIgnoreCase(email)?.let { HttpResponse.ok(it.toDTO()) } ?: HttpResponse.notFound()

    @Put("/{id}")
    suspend fun updateUser(
        id: UUID,
        @Body userDTO: UserDTO,
    ): HttpResponse<UserDTO> =
        userRepository.findById(id)?.let {
            HttpResponse.ok(
                userRepository.update(
                    it.copy(
                        name = userDTO.name,
                        email = userDTO.email,
                        roles = userDTO.roles,
                        attributes = userDTO.attributes,
                    ),
                ).toDTO(),
            )
        } ?: HttpResponse.notFound()

    @Delete("/{id}")
    suspend fun deleteUser(id: UUID): HttpResponse<String> {
        userRepository.deleteById(id)
        LOG.info("User $id has been deleted by admin")
        return HttpResponse.ok("User $id has been deleted")
    }

    @Put("/password")
    suspend fun changePassword(
        authentication: Authentication,
        @Body changePassword: ChangePasswordDTO,
    ): HttpResponse<Any> =
        userRepository.loginUser(authentication.name, changePassword.oldPassword)?.let {
            userRepository.changePassword(it.id, changePassword.oldPassword, changePassword.newPassword)
            HttpResponse.ok()
        } ?: throw BadRequestException("Wrong user info, please check password and email is correct")


}

@Introspected
data class UserAdminCriteria(val email: String?=null, val name: String?=null) {
    fun isNotEmpty() = email != null || name != null
    fun emailLower() = email?.lowercase() 
}
