package no.nav.hm.grunndata.register.bestillingsordning

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory


@Controller(BestillingsordningAdminController.API_V1_ADMIN_BESTILLINGSORDNING_REGISTRATIONS)
@Secured(Roles.ROLE_ADMIN)
@Tag(name="Admin Bestillingsordning")
class BestillingsordningAdminController(private val bestillingsordningService: BestillingsordningService ) {

    companion object {
        private val LOG = LoggerFactory.getLogger(BestillingsordningAdminController::class.java)
        const val API_V1_ADMIN_BESTILLINGSORDNING_REGISTRATIONS = "/admin/api/v1/bestillingsordning/registrations"
    }


    @Get("/{hmsArtNr}")
    suspend fun findByHmsNr(hmsArtNr: String): HttpResponse<BestillingsordningRegistrationDTO?> =
        bestillingsordningService.findByHmsArtNr(hmsArtNr)
            ?.let { HttpResponse.ok(it) }
            ?: HttpResponse.notFound()

    @Post
    suspend fun createBestillingsordning(dto: BestillingsordningRegistrationDTO, authentication: Authentication): HttpResponse<BestillingsordningRegistrationDTO> =
        bestillingsordningService.findByHmsArtNr(dto.hmsArtNr)?.let {
            throw BadRequestException("Bestillingsordning with hmsArtNr ${dto.hmsArtNr} already exists")
       } ?: run { HttpResponse.created(
            bestillingsordningService.saveAndCreateEvent(
                dto.copy(createdByUser = authentication.name, updatedByUser = authentication.name), update = false)
        )
    }


    @Put("/{hmsArtNr}")
    suspend fun updateBestillingsordningStatus(dto: BestillingsordningRegistrationDTO, authentication: Authentication): HttpResponse<BestillingsordningRegistrationDTO?> =
          bestillingsordningService.findByHmsArtNr(dto.hmsArtNr)?.let {
                HttpResponse.ok(bestillingsordningService.saveAndCreateEvent(dto.copy(updatedByUser = authentication.name), update = true))
            } ?: HttpResponse.notFound()

}