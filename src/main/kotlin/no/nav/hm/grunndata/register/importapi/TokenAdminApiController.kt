package no.nav.hm.grunndata.register.importapi

import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import no.nav.hm.grunndata.register.importapi.TokenAdminApiController.Companion.API_V1_ADMIN_IMPORT_TOKEN_API
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_IMPORT_TOKEN_API)
class TokenAdminApiController(private val tokenClient: ImportApiTokenClient,
                              @Value("\${grunndata.import.token}") private val bearerToken: String) {


    companion object {
        private val LOG = LoggerFactory.getLogger(TokenAdminApiController::class.java)
        const val API_V1_ADMIN_IMPORT_TOKEN_API = "/admin/api/v1/import/token"
    }

    @Post("/{supplierId}")
    suspend fun createSupplierToken(supplierId: UUID): String {
        LOG.info("creating token for supplier $supplierId")
        return tokenClient.createSupplierToken(supplierId, bearerToken)
    }

}