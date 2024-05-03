package no.nav.hm.grunndata.register.iso

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.Flow
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import java.time.LocalDateTime


@Secured(Roles.ROLE_ADMIN)
@Controller(IsoCategoryRegistrationAdminController.API_V1_ADMIN_ISOCATEGORY_REGISTRATIONS)
@Tag(name="Admin IsoCategory")
class IsoCategoryRegistrationAdminController(private val isoCategoryRegistrationService: IsoCategoryRegistrationService) {

    companion object {
        const val API_V1_ADMIN_ISOCATEGORY_REGISTRATIONS = "/admin/api/v1/isocategory/registrations"
    }

    @Get("/")
    suspend fun getAllCategories(): Flow<IsoCategoryRegistrationDTO> = isoCategoryRegistrationService.findAll()

    @Get("/{isocode}")
    suspend fun getCategoryByIsocode(isocode: String): HttpResponse<IsoCategoryRegistrationDTO> =
        isoCategoryRegistrationService.findByCode(isocode)?.let {
            HttpResponse.ok(it)
        } ?: HttpResponse.notFound()


    @Post("/")
    suspend fun createIsoCategory(dto: IsoCategoryRegistrationDTO, authentication: Authentication): HttpResponse<IsoCategoryRegistrationDTO> =
        isoCategoryRegistrationService.findByCode(dto.isoCode)?.let {
            throw BadRequestException("IsoCategory ${dto.isoCode} already exists")
        } ?: HttpResponse.created(isoCategoryRegistrationService.save(dto.copy(createdByUser = authentication.name,
            updatedByUser = authentication.name)))


    @Put("/{isocode}")
    suspend fun updateCategoryByIsocode(isocode: String, dto: IsoCategoryRegistrationDTO, authentication: Authentication): HttpResponse<IsoCategoryRegistrationDTO> =
        isoCategoryRegistrationService.findByCode(isocode)?.let { inDb ->
            HttpResponse.ok(isoCategoryRegistrationService.update(dto.copy(created = inDb.created,
                createdBy = inDb.createdBy, createdByUser = inDb.createdByUser, updated = LocalDateTime.now(),
                updatedByUser = authentication.name)))
        } ?: HttpResponse.notFound()
}