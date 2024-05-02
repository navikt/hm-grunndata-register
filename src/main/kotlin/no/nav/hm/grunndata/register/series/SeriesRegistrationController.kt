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
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(SeriesController.API_V1_SERIES)
class SeriesController(private val seriesRegistrationService: SeriesRegistrationService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesController::class.java)
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
                            ),
                        true,
                    ),
                )
            } ?: run {
                LOG.warn("Series with id $id does not exist")
                HttpResponse.notFound()
            }
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
