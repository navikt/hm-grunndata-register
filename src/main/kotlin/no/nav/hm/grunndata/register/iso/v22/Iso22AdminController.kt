package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
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
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Secured(Roles.ROLE_ADMIN)
@Controller(Iso22AdminController.API_V1_ADMIN_ISO22)
@Tag(name="Admin IsoCategory v22")
class Iso22AdminController(private val iso22Repository: Iso22Repository) {

    
    @Get("/")
    suspend fun getAllIsos(): List<Iso22> {
        return iso22Repository.findAll().toList()
    }


    @Post("/")
    suspend fun createIso(@Body iso: Iso22DTO, authentication: Authentication): HttpResponse<Iso22DTO> =
        iso22Repository.findByIsoCode(iso.isoCode)?.let {
            throw BadRequestException("Iso22 ${iso.isoCode} already exists")
        } ?: HttpResponse.created(iso22Repository.save(iso.copy(createdByUser = authentication.name,
            updatedByUser = authentication.name, created = LocalDateTime.now(), updated = LocalDateTime.now()).toEntity()).toDTO())


    @Put("/{isocode}")
    suspend fun updateIsoByIsocode(isocode: String, @Body iso: Iso22DTO, authentication: Authentication): HttpResponse<Iso22DTO> =
        iso22Repository.findByIsoCode(isocode)?.let { inDb ->
            HttpResponse.ok(iso22Repository.update(iso.copy(id = inDb.id, created = inDb.created,
                createdByUser = inDb.createdByUser, updatedByUser = authentication.name, updated = LocalDateTime.now()).toEntity()).toDTO())
        } ?: HttpResponse.notFound()

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(Iso22AdminController::class.java)
        const val API_V1_ADMIN_ISO22 = "/admin/api/v1/iso22"
    }

    fun Iso22.toDTO(): Iso22DTO = Iso22DTO(
        id = this.id,
        isoCode = this.isoCode,
        isoTitle = this.isoTitle,
        isoText = this.isoText,
        isoTranslations = this.isoTranslations,
        searchWords = this.searchWords,
        isoType = this.isoType,
        createdByUser = this.createdByUser,
        updatedByUser = this.updatedByUser,
        createdBy = this.createdBy,
        updatedBy = this.updatedBy,
        created = this.created,
        updated = this.updated
    )

    fun Iso22DTO.toEntity(): Iso22 = Iso22(
        id = this.id,
        isoCode = this.isoCode,
        isoTitle = this.isoTitle,
        isoText = this.isoText,
        isoTranslations = this.isoTranslations,
        searchWords = this.searchWords,
        isoType = this.isoType,
        createdByUser = this.createdByUser,
        updatedByUser = this.updatedByUser,
        createdBy = this.createdBy,
        updatedBy = this.updatedBy,
        created = this.created,
        updated = this.updated
    )
}