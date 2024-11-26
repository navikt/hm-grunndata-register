package no.nav.hm.grunndata.register.product.attributes.digitalsoknadsortiment

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory


@Controller(DigitalSoknadSortimentAdminController.API_V1_ADMIN_DIGITALSOKNADSORTIMENT_REGISTRATIONS)
@Secured(Roles.ROLE_ADMIN)
@Tag(name="Admin Digital Soknad")
class DigitalSoknadSortimentAdminController(private val digitalSoknadSortimentService: DigitalSoknadSortimentService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(DigitalSoknadSortimentAdminController::class.java)
        const val API_V1_ADMIN_DIGITALSOKNADSORTIMENT_REGISTRATIONS = "/admin/api/v1/digitalsoknadsortiment/registrations"
    }

    @Post("/manual-sync")
    suspend fun manualSync(authentication: Authentication): HttpResponse<Unit> =
        digitalSoknadSortimentService.importAndUpdateDb().run {
            HttpResponse.ok()
        }
}
