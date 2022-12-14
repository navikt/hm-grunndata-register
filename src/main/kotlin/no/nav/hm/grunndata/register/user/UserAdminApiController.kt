package no.nav.hm.grunndata.register.user

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller("/api/v1/admin/user")
class UserAdminApiController(private val userRepository: UserRepository) {

    companion object {
        private val LOG = LoggerFactory.getLogger(UserAdminApiController::class.java)
    }

    @Get("/{?params*}")
    suspend fun getUsers(@QueryValue params: HashMap<String, String>?, pageable: Pageable): Page<UserDTO> =
        userRepository.findAll(buildCriteriaSpec(params), pageable).map { it.toDTO() }


    private fun buildCriteriaSpec(params: java.util.HashMap<String, String>?): PredicateSpecification<User>? =
        params?.let {
            where {
                if (params.contains("email")) root[User::email] eq params["email"]
                if (params.contains("name")) criteriaBuilder.like(root[User::name], params["name"])
                if (params.contains("supplierId"))
                    criteriaBuilder.equal(
                        criteriaBuilder.function(
                            "jsonb_extract_path_text", String::class.java,
                            root[User::attributes], criteriaBuilder.literal("name")
                        ), params["supplierId"]
                    )
            }
        }

}

