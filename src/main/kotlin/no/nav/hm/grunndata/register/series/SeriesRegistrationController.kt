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
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_SUPPLIER)
@Controller(SeriesRegistrationController.API_V1_SERIES)
@Tag(name = "Vendor Series")
class SeriesRegistrationController(private val seriesRegistrationService: SeriesRegistrationService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationController::class.java)
        const val API_V1_SERIES = "/vendor/api/v1/series"
    }

    @Get("/{?params*}")
    suspend fun getSeries(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<SeriesRegistrationDTO> {
        return seriesRegistrationService.findAll(buildCriteriaSpec(params, authentication.supplierId()), pageable)
    }

    @Post("/")
    suspend fun createSeries(
        @Body seriesRegistrationDTO: SeriesRegistrationDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        if (seriesRegistrationDTO.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            HttpResponse.unauthorized()
        } else {
            seriesRegistrationService.findById(seriesRegistrationDTO.id)?.let {
                LOG.warn("Series with id ${seriesRegistrationDTO.id} already exists")
                HttpResponse.badRequest()
            } ?: run {
                HttpResponse.created(
                    seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                        seriesRegistrationDTO,
                        false,
                    ),
                )
            }
        }

    @Post("/draft")
    suspend fun createDraftSeries(authentication: Authentication): HttpResponse<SeriesRegistrationDTO> {
        return HttpResponse.ok(seriesRegistrationService.createDraft(authentication.supplierId(), authentication))
    }

    @Post("/draftWith")
    suspend fun draftSeriesWith(
        @Body draftWith: SeriesDraftWithDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        return HttpResponse.ok(
            seriesRegistrationService.createDraftWith(
                authentication.supplierId(),
                authentication,
                draftWith,
            ),
        )
    }

    @Get("/{id}")
    suspend fun readSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        seriesRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())?.let {
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
        if (seriesRegistrationDTO.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            HttpResponse.unauthorized()
        } else {
            seriesRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())?.let { inDb ->
                HttpResponse.ok(
                    seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                        seriesRegistrationDTO
                            .copy(
                                adminStatus = inDb.adminStatus,
                                created = inDb.created,
                                createdByUser = inDb.createdByUser,
                                updated = LocalDateTime.now(),
                                updatedByUser = authentication.name,
                                updatedBy = REGISTER,
                            ),
                        true,
                    ),
                )
            } ?: run {
                LOG.warn("Series with id $id does not exist")
                HttpResponse.notFound()
            }
        }

    @Put("/serie-til-godkjenning/{seriesUUID}")
    suspend fun setSeriesToBeApproved(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID)

        if (seriesToUpdate?.draftStatus != DraftStatus.DRAFT) throw BadRequestException("series is marked as done")

        val updatedSeries =
            seriesToUpdate.copy(
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.PENDING,
                updated = LocalDateTime.now(),
                updatedBy = REGISTER,
            )

        val updated =
            seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedSeries, isUpdate = true)

        return HttpResponse.ok(updated)
    }

    @Put("/series_to-draft/{seriesUUID}")
    suspend fun setPublishedSeriesToDraft(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val updated = seriesRegistrationService.setPublishedSeriesToDraftStatus(seriesUUID, authentication)

        return HttpResponse.ok(updated)
    }

    @Delete("/{seriesUUID}")
    suspend fun deleteSeries(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()

        if (seriesToUpdate.draftStatus != DraftStatus.DRAFT) throw BadRequestException("series is not a draft")

        val updatedSeries =
            seriesToUpdate.copy(
                status = SeriesStatus.DELETED,
                expired = LocalDateTime.now(),
                updatedByUser = authentication.name,
                updatedBy = REGISTER,
                updated = LocalDateTime.now(),
            )

        val updated =
            seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedSeries, isUpdate = true)

        return HttpResponse.ok(updated)
    }

    private fun buildCriteriaSpec(
        params: HashMap<String, String>?,
        supplierId: UUID,
    ): PredicateSpecification<SeriesRegistration> =
        params?.let {
            where {
                root[SeriesRegistration::supplierId] eq supplierId
                if (params.contains("status")) {
                    val statusList: List<SeriesStatus> = params["status"]!!.split(",").map { SeriesStatus.valueOf(it) }
                    root[SeriesRegistration::status] inList statusList
                }
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
        } ?: where {
            root[SeriesRegistration::supplierId] eq supplierId
        }
}

data class SeriesDraftWithDTO(val title: String, val isoCategory: String)
