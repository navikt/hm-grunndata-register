package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(SeriesRegistrationAdminController.API_V1_SERIES)
@Tag(name = "Admin Series")
class SeriesRegistrationAdminController(private val seriesRegistrationService: SeriesRegistrationService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationAdminController::class.java)
        const val API_V1_SERIES = "/admin/api/v1/series"
    }

    @Get("/{?params*}")
    suspend fun getSeries(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<SeriesRegistrationDTO> {
        return seriesRegistrationService.findAll(buildCriteriaSpec(params), pageable)
    }

    private fun buildCriteriaSpec(params: java.util.HashMap<String, String>?): PredicateSpecification<SeriesRegistration>? =
        params?.let {
            where {
                if (params.contains("adminStatus")) root[SeriesRegistration::adminStatus] eq AdminStatus.valueOf(params["adminStatus"]!!)
                if (params.contains("status")) {
                    val statusList: List<SeriesStatus> =
                        params["status"]!!.split(",").map { SeriesStatus.valueOf(it) }
                    root[SeriesRegistration::status] inList statusList
                }
                if (params.contains("supplierId")) root[SeriesRegistration::supplierId] eq UUID.fromString(params["supplierId"]!!)
                if (params.contains("draft")) root[SeriesRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
                if (params.contains("createdByUser")) root[SeriesRegistration::createdByUser] eq params["createdByUser"]
                if (params.contains("updatedByUser")) root[SeriesRegistration::updatedByUser] eq params["updatedByUser"]
                if (params.contains("excludedStatus")) {
                    root[SeriesRegistration::status] ne params["excludedStatus"]
                }
            }.and { root, criteriaBuilder ->
                if (params.contains("title")) {
                    val term = params["title"]!!.lowercase(Locale.getDefault())
                    criteriaBuilder.like(
                        root[SeriesRegistration::titleLowercase],
                        LiteralExpression("%$term%"),
                    )
                } else {
                    null
                }
            }
        }

    @Get("/{id}")
    suspend fun readSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        seriesRegistrationService.findById(id)?.let {
            HttpResponse.ok(it)
        } ?: run {
            LOG.warn("Series with id $id does not exist")
            HttpResponse.notFound()
        }

    @Put("/{id}")
    suspend fun updateSeries(
        @PathVariable id: UUID,
        @Body seriesRegistrationDTO: SeriesRegistrationDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =

        seriesRegistrationService.findById(id)?.let { inDb ->
            HttpResponse.ok(
                seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                    seriesRegistrationDTO
                        .copy(
                            adminStatus = inDb.adminStatus,
                            created = inDb.created,
                            createdByUser = inDb.createdByUser,
                            updated = LocalDateTime.now(),
                            updatedByUser = authentication.name,
                        ),
                    true,
                ),
            )
        } ?: run {
            LOG.warn("Series with id $id does not exist")
            HttpResponse.notFound()
        }

    @Get("/to-approve")
    suspend fun findSeriesPendingApprove(
        @QueryValue params: java.util.HashMap<String, String>?,
        pageable: Pageable,
    ): Page<SeriesRegistrationDTO> = seriesRegistrationService.findSeriesToApprove(pageable)

    @Put("/approve/{id}")
    suspend fun approveSeries(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        seriesRegistrationService.findById(id)?.let {
            if (it.adminStatus == AdminStatus.APPROVED) throw BadRequestException("$id is already approved")
            if (it.draftStatus != DraftStatus.DONE) throw BadRequestException("Series is not done")
            if (it.status != SeriesStatus.ACTIVE) throw BadRequestException("SeriesStatus should be Active")
            val dto =
                seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                    it.copy(
                        adminStatus = AdminStatus.APPROVED,
                        updated = LocalDateTime.now(),
                        updatedBy = REGISTER,
                    ),
                    isUpdate = true,
                )
            HttpResponse.ok(dto)
        } ?: HttpResponse.notFound()

    @Delete("/{id}")
    suspend fun deleteSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        seriesRegistrationService.findById(id)
            ?.let {
                LOG.info("Deleting product ${it.id}")
                val dto =
                    seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                        it.copy(
                            status = SeriesStatus.DELETED,
                            updatedByUser = authentication.name,
                            updatedBy = REGISTER,
                        ),
                        isUpdate = true,
                    )
                HttpResponse.ok(dto)
            }
            ?: HttpResponse.notFound()
}
