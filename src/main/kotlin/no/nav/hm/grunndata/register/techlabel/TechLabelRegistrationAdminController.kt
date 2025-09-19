package no.nav.hm.grunndata.register.techlabel

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.mapSuspend
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(TechLabelRegistrationAdminController.API_V1_ADMIN_TECHLABEL_REGISTRATIONS)
@Tag(name="Admin TechLabel")
class TechLabelRegistrationAdminController(private val techLabelRegistrationService: TechLabelRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelRegistrationAdminController::class.java)
        const val API_V1_ADMIN_TECHLABEL_REGISTRATIONS = "/admin/api/v1/techlabel/registrations"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @Get("/")
    suspend fun findTechLabels(@RequestBean criteria: TechLabelCriteria, pageable: Pageable, authentication: Authentication):
        Page<TechLabelRegistrationDTO> = techLabelRegistrationService
            .findAll(buildCriteriaSpec(criteria), pageable)
            .mapSuspend { it.toDTO() }

    private fun buildCriteriaSpec(criteria: TechLabelCriteria): PredicateSpecification<TechLabelRegistration>? =
        if (criteria.isNotEmpty()) {
            where {
                criteria.label?.let { root[TechLabelRegistration::label] eq it }
                criteria.isoCode?.let { root[TechLabelRegistration::isoCode] eq it }
                criteria.unit?.let { root[TechLabelRegistration::unit] eq it }
                criteria.type?.let { root[TechLabelRegistration::type] eq it }
            }
        }
        else null

    @Get("/{id}")
    suspend fun getTechLabelById(id: UUID): HttpResponse<TechLabelRegistrationDTO> =
        techLabelRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(it.toDTO())
            }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createTechLabel(dto: TechLabelRegistrationDTO, authentication: Authentication): HttpResponse<TechLabelRegistrationDTO> =
        if (techLabelRegistrationService.findById(dto.id)!=null) throw BadRequestException("TechLabel ${dto.id} already exists")
        else  HttpResponse.created(
            techLabelRegistrationService.save(
                dto.copy(
                    created = LocalDateTime.now(),
                    createdBy = authentication.name,
                    createdByUser = authentication.name,
                    updated = LocalDateTime.now(),
                    updatedByUser = authentication.name
                )
            ).toDTO()
        )

    @Put("/{id}")
    suspend fun updateTechLabel(id: UUID, dto: TechLabelRegistrationDTO): HttpResponse<TechLabelRegistrationDTO> =
        techLabelRegistrationService.findById(id)?.let { inDb ->
            val updated = techLabelRegistrationService.update(dto.copy(created = inDb.created,
                createdBy = inDb.createdBy, createdByUser = inDb.createdByUser, updated = LocalDateTime.now(),
                updatedByUser = inDb.updatedByUser)
            )
            if (inDb.label != dto.label || inDb.unit != dto.unit) {
                LOG.info("Updated TechLabel with id=$id, changed label or unit, update products using this label")
                // Trigger update on a new thread to not block the user response using kotlin coroutine
                coroutineScope.launch {
                    techLabelRegistrationService.changeProductsTechDataWithTechLabel(inDb.label, inDb.unit, inDb.isoCode, updated)
                }
            }
            HttpResponse.ok(updated.toDTO())
        } ?: HttpResponse.notFound()


}

@Introspected
data class TechLabelCriteria(
    val label: String? = null,
    val type: TechLabelType? = null,
    val unit: String? = null,
    val isoCode: String? = null) {

    fun isNotEmpty() = label != null || type != null || unit != null || isoCode != null
}
