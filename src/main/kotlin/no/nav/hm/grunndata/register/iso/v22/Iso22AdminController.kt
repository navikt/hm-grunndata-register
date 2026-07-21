package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller(Iso22AdminController.API_V1_ADMIN_ISO22)
@Tag(name="Admin IsoCategory")
class Iso22AdminController(private val iso22Repository: Iso22Repository) {


    @Post("/")
    suspend fun updateIso22(@Body isos: List<Iso22>) {
        LOG.info("Got iso22: $isos")
        isos.forEach { iso22 ->
            iso22Repository.findByIsoCode(iso22.isoCode)?.let {
                iso22Repository.update(iso22.copy(id = it.id, created = it.created))
            } ?:run {
                iso22Repository.save(iso22)
            }
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(Iso22AdminController::class.java)
        const val API_V1_ADMIN_ISO22 = "/api/v1/admin/iso22"
    }
}