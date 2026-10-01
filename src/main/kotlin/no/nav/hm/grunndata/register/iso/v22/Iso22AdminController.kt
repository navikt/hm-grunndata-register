package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import java.time.LocalDateTime
import kotlin.collections.copy

@Secured(Roles.ROLE_ADMIN)
@Controller(Iso22AdminController.ADMIN_API_V22_ISO)
@Tag(name = "Admin IsoCategory v22")
class Iso22AdminController(
    private val iso22Repository: Iso22Repository,
    private val iso22Service: Iso22Service,
) {


    @Get("/")
    suspend fun getAllIsos(): List<Iso22> {
        return iso22Repository.findAll().toList()
    }


    @Get("/{isocode}")
    suspend fun getIsoByIsocode(isocode: String): HttpResponse<Iso22DTO> =
        iso22Repository.findByIsoCode(isocode)?.let { HttpResponse.ok(it.toDTO()) } ?: HttpResponse.notFound()


    @Post("/")
    suspend fun createIso(@Body iso: Iso22DTO, authentication: Authentication): HttpResponse<Iso22DTO> =
        iso22Repository.findByIsoCode(iso.isoCode)?.let {
            throw BadRequestException("Iso22 ${iso.isoCode} already exists")
        } ?: iso22Repository.save(
            iso.copy(
                createdByUser = authentication.name,
                updatedByUser = authentication.name, created = LocalDateTime.now(), updated = LocalDateTime.now()
            ).toEntity()
        )
            .also { iso22Service.upsert(it.toRapidDTO()) }
            .let { HttpResponse.created(it.toDTO()) }


    @Put("/{isocode}")
    suspend fun updateIsoByIsocode(
        isocode: String,
        @Body iso: Iso22DTO,
        authentication: Authentication
    ): HttpResponse<Iso22DTO> {

        if (isocode != iso.isoCode) {
            throw BadRequestException("IsoCode in path does not match IsoCode in body")
        }

        return iso22Repository.findByIsoCode(isocode)?.let { inDb ->
            iso22Repository.update(
                iso.copy(
                    id = inDb.id,
                    created = inDb.created,
                    createdByUser = inDb.createdByUser,
                    updatedByUser = authentication.name,
                    updated = LocalDateTime.now()
                ).toEntity()
            )
                .also { iso22Service.upsert(it.toRapidDTO()) }
                .let { HttpResponse.ok(it.toDTO()) }
        } ?: HttpResponse.notFound()

    }


    companion object {
        const val ADMIN_API_V22_ISO = "/admin/api/v22/isocategory"
    }

    fun Iso22.toDTO(): Iso22DTO = Iso22DTO(
        id = this.id,
        isoCode = this.isoCode,
        isoTitle = this.isoTitle,
        isoText = this.isoText,
        level = getLevelFromIsoCode(this.isoCode),
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