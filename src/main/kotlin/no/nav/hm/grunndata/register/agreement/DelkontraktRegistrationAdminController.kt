package no.nav.hm.grunndata.register.agreement

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.userId
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(DelkontraktRegistrationAdminController.API_V1_ADMIN_DELKONTRAKT_REGISTRATIONS)
class DelkontraktRegistrationAdminController(private val delkontraktRegistrationService: DelkontraktRegistrationService,
                                             private val agreementService: AgreementRegistrationService) {

    companion object {
        const val API_V1_ADMIN_DELKONTRAKT_REGISTRATIONS = "/admin/api/v1/agreement/delkontrakt/registrations"
        private val LOG = LoggerFactory.getLogger(DelkontraktRegistrationAdminController::class.java)
    }

    @Get("/agreement/{agreementId}")
    suspend fun findByAgreementId(agreementId: UUID): List<DelkontraktRegistrationDTO> =
        delkontraktRegistrationService.findByAgreementId(agreementId)


    @Get("/{id}")
    suspend fun findByDelkontraktId(id: UUID): HttpResponse<DelkontraktRegistrationDTO?> =
        delkontraktRegistrationService.findById(id)?.let { HttpResponse.ok(it) } ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createDelkontrakt(
        dto: DelkontraktRegistrationDTO,
        authentication: Authentication
    ): HttpResponse<DelkontraktRegistrationDTO> =
        delkontraktRegistrationService.findById(dto.id)?.let {
            throw BadRequestException("Delkontrakt ${dto.id} already exists")
        } ?: HttpResponse.created(delkontraktRegistrationService.save(dto))

    @Put("/{id}")
    suspend fun updateDelkontrakt(
        id: UUID,
        dto: DelkontraktRegistrationDTO,
        authentication: Authentication
    ): HttpResponse<DelkontraktRegistrationDTO> =
        delkontraktRegistrationService.findById(id)?.let {
            HttpResponse.ok(delkontraktRegistrationService.update(dto))
        } ?: HttpResponse.notFound(
    )

    @Delete("/id")
    suspend fun deleteDelkontraktById(id: UUID, authentication: Authentication): HttpResponse<Unit> {
        LOG.info("deleting delkontrakt $id by user ${authentication.userId()}")
        return delkontraktRegistrationService.findById(id)?.let { inDb ->
                val agreement = agreementService.findById(inDb.agreementId)
                if (agreement!=null && agreement.draftStatus == DraftStatus.DRAFT) {
                    delkontraktRegistrationService.deleteById(id)
                    HttpResponse.noContent()
                } else {
                    throw BadRequestException("Delkontrakt $id cannot be deleted")
                }
        } ?: HttpResponse.notFound()
    }

}