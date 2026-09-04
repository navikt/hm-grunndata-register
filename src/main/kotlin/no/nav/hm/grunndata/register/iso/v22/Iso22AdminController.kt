package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import no.nav.hm.grunndata.register.error.BadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller(Iso22AdminController.API_V1_ADMIN_ISO22)
@Tag(name="Admin IsoCategory")
class Iso22AdminController(private val iso22Repository: Iso22Repository) {

    
    @Get("/")
    suspend fun getAllIsos(): List<Iso22> {
        return iso22Repository.findAll().toList()
    }


    @Post("/")
    suspend fun createIso(@Body iso: Iso22, authentication: Authentication): HttpResponse<Iso22> =
        iso22Repository.findByIsoCode(iso.isoCode)?.let {
            throw BadRequestException("Iso22 ${iso.isoCode} already exists")
        } ?: HttpResponse.created(iso22Repository.save(iso.copy(createdByUser = authentication.name,
            updatedByUser = authentication.name, created = LocalDateTime.now(), updated = LocalDateTime.now())))


    @Put("/{isocode}")
    suspend fun updateIsoByIsocode(isocode: String, @Body iso: Iso22, authentication: Authentication): HttpResponse<Iso22> =
        iso22Repository.findByIsoCode(isocode)?.let { inDb ->
            HttpResponse.ok(iso22Repository.update(iso.copy(id = inDb.id, created = inDb.created,
                createdByUser = inDb.createdByUser, updatedByUser = authentication.name, updated = LocalDateTime.now())))
        } ?: HttpResponse.notFound()

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(Iso22AdminController::class.java)
        const val API_V1_ADMIN_ISO22 = "/api/v1/admin/iso22"
    }


}