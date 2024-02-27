package no.nav.hm.grunndata.register.agreement

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(DelkontraktRegistrationAdminController.API_V1_ADMIN_DELKONTRAKT_REGISTRATIONS)
class DelkontraktRegistrationAdminController(private val delkontraktRegistrationService: DelkontraktRegistrationService) {

    companion object {
        const val API_V1_ADMIN_DELKONTRAKT_REGISTRATIONS = "/admin/api/v1/agreement/delkontrakt/registrations"
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

}