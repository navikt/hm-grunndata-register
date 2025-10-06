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
@Tag(name = "Admin TechLabel")
class TechLabelRegistrationAdminController(
    private val techLabelRegistrationService: TechLabelRegistrationService,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelRegistrationAdminController::class.java)
        const val API_V1_ADMIN_TECHLABEL_REGISTRATIONS = "/admin/api/v1/techlabel/registrations"
    }

    @Get("/")
    suspend fun findTechLabels(
        @RequestBean criteria: TechLabelCriteria,
        pageable: Pageable,
        authentication: Authentication
    ):
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
        } else null

    @Get("/{id}")
    suspend fun getTechLabelById(id: UUID): HttpResponse<TechLabelRegistrationDTO> =
        techLabelRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(it.toDTO())
            }
            ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createTechLabel(
        @Body dto: TechLabelCreateUpdateDTO,
        authentication: Authentication
    ): HttpResponse<TechLabelRegistrationDTO> =
        if (techLabelRegistrationService.findByLabelAndIsoCode(dto.label, dto.isoCode) != null) {
            throw BadRequestException("TechLabel with label='${dto.label}' and isocode='${dto.isoCode}' already exists")
        } else if (dto.type == TechLabelType.N && (dto.unit == null || dto.unit.isEmpty())) {
            throw BadRequestException("TechLabel with type=NUMERIC must have a unit")
        } else if (dto.isoCode.length<6 || dto.isoCode.length>8) {
            throw BadRequestException("TechLabel must have a valid isoCode with length between 6 and 8 characters")
        }
        else
            HttpResponse.created(
                techLabelRegistrationService.save(
                    TechLabelRegistration(
                        label = dto.label,
                        guide = dto.label,
                        definition = null,
                        isoCode = dto.isoCode,
                        type = dto.type,
                        unit = dto.unit,
                        sort = 0,
                        options = dto.options,
                        createdByUser = authentication.name,
                        updatedByUser = authentication.name
                    )
                ).toDTO()
            )

    @Put("/{id}")
    suspend fun updateTechLabel(
        id: UUID,
        @Body dto: TechLabelCreateUpdateDTO,
        authentication: Authentication
    ): HttpResponse<TechLabelRegistrationDTO> =
        techLabelRegistrationService.findById(id)?.let { inDb ->
            val updated = techLabelRegistrationService.update(
                inDb.copy(
                    label = dto.label,
                    guide = dto.label,
                    isoCode = dto.isoCode,
                    type = dto.type,
                    unit = dto.unit,
                    options = dto.options,
                    updated = LocalDateTime.now(),
                    updatedByUser = authentication.name
                )
            )
            if (inDb.label != dto.label || inDb.unit != dto.unit) {
                LOG.info("Updated TechLabel with id=$id, changed label or unit, update products using this label")
                try {
                    coroutineScope.launch {
                        techLabelRegistrationService.changeProductsTechDataWithTechLabel(
                            inDb.label,
                            inDb.unit,
                            inDb.isoCode,
                            updated
                        )
                    }
                } catch (e: Exception) {
                    LOG.error("Error updating products with new techLabel ${e.message}", e)
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
    val isoCode: String? = null
) {

    fun isNotEmpty() = label != null || type != null || unit != null || isoCode != null
}

@Introspected
data class TechLabelCreateUpdateDTO(
    val label: String,
    val isoCode: String,
    val type: TechLabelType,
    val unit: String?,
    val options: List<String> = emptyList(),
)