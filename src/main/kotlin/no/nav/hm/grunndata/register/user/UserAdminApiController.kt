package no.nav.hm.grunndata.register.user

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import no.nav.hm.grunndata.rapid.dto.AttributeNames
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.user.UserAdminApiController.Companion.API_V1_ADMIN_USER_REGISTRATIONS
import no.nav.hm.grunndata.register.user.UserAttribute.SUPPLIER_ID
import org.slf4j.LoggerFactory
import java.util.*


@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_USER_REGISTRATIONS)
class UserAdminApiController(private val userRepository: UserRepository,
                             private val supplierRepository: SupplierRepository
) {

    companion object {
        const val API_V1_ADMIN_USER_REGISTRATIONS = "/api/v1/admin/users"
        private val LOG = LoggerFactory.getLogger(UserAdminApiController::class.java)
    }

    @Get("/{?params*}")
    suspend fun getUsers(@QueryValue params: HashMap<String, String>?, pageable: Pageable): Page<UserDTO> =
        userRepository.findAll(buildCriteriaSpec(params), pageable).map { it.toDTO() }


    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<User>? =
        params?.let {
            where {
                if (params.contains("email")) root[User::email] eq params["email"]
                if (params.contains("name")) criteriaBuilder.like(root[User::name], params["name"])
            }
        }

    @Get("/supplierId/{supplierId}")
    suspend fun getUsersBySupplierId(supplierId: UUID): List<UserDTO> =
        userRepository.getUsersBySupplierId(supplierId.toString()).map { it.toDTO() }

    @Post("/")
    suspend fun createUser(@Body dto: UserRegistrationDTO): HttpResponse<UserDTO> {
        LOG.info("Creating user ${dto.id} ")
        if (dto.attributes.containsKey(SUPPLIER_ID)) {
            val supplierId = UUID.fromString(dto.attributes[SUPPLIER_ID])
            supplierRepository.findById(supplierId) ?: throw Exception("Unknown supplier id $supplierId")
        }
        val entity = User(
            id = dto.id, name = dto.name, email = dto.email, token = dto.password, roles = dto.roles,
            attributes = dto.attributes
        )
        userRepository.createUser(entity)
        return HttpResponse.created(entity.toDTO())
    }


    @Get("/{id}")
    suspend fun getUser(id:UUID) : HttpResponse<UserDTO> =
        userRepository.findById(id)
            ?.let {
                HttpResponse.ok(it.toDTO())
            } ?: HttpResponse.notFound()


    @Get("/email/{email}")
    suspend fun getUserByEmail(email: String): HttpResponse<UserDTO> =
        userRepository.findByEmail(email)?.let { HttpResponse.ok(it.toDTO())}  ?: HttpResponse.notFound()


    @Put("/{id}")
    suspend fun updateUser(id: UUID, @Body userDTO: UserDTO): HttpResponse<UserDTO> =
        userRepository.findById(id)?.let { HttpResponse.ok(userRepository.update(it.copy(name = userDTO.name, email = userDTO.email,
            roles = userDTO.roles, attributes = userDTO.attributes)).toDTO()) } ?: HttpResponse.notFound()

}

