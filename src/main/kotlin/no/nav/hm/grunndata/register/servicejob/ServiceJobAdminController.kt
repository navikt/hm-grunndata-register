package no.nav.hm.grunndata.register.servicejob

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.logging.sanitizeForLog
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
        LOG.info("Getting service jobs for agreement {{}} by {}", agreementId, authentication.userId())
        return serviceJobService.findByAgreementId(agreementId)
    }

    @Put("/{id}")
    suspend fun updateServiceJob(
        id: UUID,
        @Body request: UpdateServiceJobRequest,
        authentication: Authentication,
    ): HttpResponse<ServiceJobDTO> {
        LOG.info("Updating service job {} by {} with title='{}' hmsArtNr='{}'", id, authentication.userId(), request.title.sanitizeForLog(), request.hmsNr.sanitizeForLog())
        val updated = serviceJobService.updateServiceJobTitleOrHmsArtNr(id, request.title, request.hmsNr)
            ?: return HttpResponse.notFound()
        return HttpResponse.ok(updated)
    }

    @Delete("/{id}")
    suspend fun deleteServiceJob(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        LOG.info("Deleting service job {} by {}", id, authentication.userId())
        val deleted = serviceJobService.deleteById(id)
        return if (deleted) HttpResponse.noContent() else HttpResponse.notFound()
    }
}

data class UpdateServiceJobRequest(
    val title: String? = null,
    val hmsNr: String? = null,
)
