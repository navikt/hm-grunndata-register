package no.nav.hm.grunndata.register.techlabel

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import kotlinx.coroutines.flow.Flow
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(TechLabelRegistrationAdminController.API_V1_ADMIN_TECHLABEL_REGISTRATIONS)
class TechLabelRegistrationAdminController(private val techLabelRegistrationService: TechLabelRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelRegistrationAdminController::class.java)
        const val API_V1_ADMIN_TECHLABEL_REGISTRATIONS = "/admin/api/v1/techlabel/registrations"
    }

    @Get("/")
    suspend fun getAllTechLabels(): Flow<TechLabelRegistrationDTO> = techLabelRegistrationService.findAll()

    @Get("/{id}")
    suspend fun getTechLabelById(id: UUID): HttpResponse<TechLabelRegistrationDTO> =
        techLabelRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(it)
            }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createTechLabel(dto: TechLabelRegistrationDTO, authentication: Authentication): HttpResponse<TechLabelRegistrationDTO> =
        techLabelRegistrationService.findById(dto.id)?.let {
            throw BadRequestException("TechLabel ${dto.id} already exists")
        } ?: HttpResponse.created(techLabelRegistrationService.save(dto.copy(createdByUser = authentication.name,
            updatedByUser = authentication.name)))

    @Put("/{id}")
    suspend fun updateTechLabel(id: UUID, dto: TechLabelRegistrationDTO): HttpResponse<TechLabelRegistrationDTO> =
        techLabelRegistrationService.findById(id)?.let { inDb ->
            HttpResponse.ok(techLabelRegistrationService.update(dto.copy(created = inDb.created,
                createdBy = inDb.createdBy, createdByUser = inDb.createdByUser, updated = LocalDateTime.now(),
                updatedByUser = inDb.updatedByUser)))
        } ?: HttpResponse.notFound()
}
