package no.nav.hm.grunndata.register.servicejob

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.userId
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(ServiceJobAdminController.ADMIN_API_V1_SERVICE_JOB)
@Tag(name = "Admin Service Job")
class ServiceJobAdminController(
    private val serviceJobService: ServiceJobService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ServiceJobAdminController::class.java)
        const val ADMIN_API_V1_SERVICE_JOB = "/admin/api/v1/service-job"
    }

    @Get("/agreement/{agreementId}")
    suspend fun getServiceJobsByAgreementId(
        agreementId: UUID,
        authentication: Authentication,
    ): List<ServiceJobDTO> {
        LOG.info("Getting service jobs for agreement {$agreementId} by ${authentication.userId()}")
        return serviceJobService.findByAgreementId(agreementId)
    }
}
