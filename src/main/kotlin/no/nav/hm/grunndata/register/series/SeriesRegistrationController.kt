package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
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
import jakarta.persistence.criteria.Predicate
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

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

    @Get("/v2/{id}")
    suspend fun readSeriesV2(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTOV2> {
        return seriesRegistrationService.findByIdAndSupplierIdV2(id, authentication.supplierId())?.let {
            HttpResponse.ok(it)
        } ?: run {
            LOG.warn("Series with id $id does not exist")
            HttpResponse.notFound()
        }
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

    @Put("/v2/{id}")
    suspend fun updateSeriesV2(
        @PathVariable id: UUID,
        @Body updateSeriesRegistrationDTO: UpdateSeriesRegistrationDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =

        seriesRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())?.let { inDb ->
            if (inDb.supplierId != authentication.supplierId()) {
                LOG.warn("SupplierId in request does not match authenticated supplierId")
                return HttpResponse.unauthorized()
            }

            HttpResponse.ok(
                seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                    inDb
                        .copy(
                            title = updateSeriesRegistrationDTO.title ?: inDb.title,
                            text = updateSeriesRegistrationDTO.text ?: inDb.text,
                            updated = LocalDateTime.now(),
                            updatedByUser = authentication.name
                        ),
                    true,
                ),
            )
        } ?: run {
            LOG.warn("Series with id $id does not exist")
            HttpResponse.notFound()
        }

    @Put("/request-approval/{seriesUUID}")
    suspend fun requestApproval(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()

        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        if (seriesToUpdate.draftStatus != DraftStatus.DRAFT) throw BadRequestException("series is marked as done")

        val updated =
            seriesRegistrationService.requestApprovalForSeriesAndVariants(seriesToUpdate)

        LOG.info("set series to pending approval: $seriesUUID")
        return HttpResponse.ok(updated)
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

    @Put("/series_ready-for-approval/{seriesUUID}")
    suspend fun setSeriesReadyForApproval(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()
        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        val updated = seriesRegistrationService.setSeriesToDraftStatus(seriesToUpdate, authentication)

        return HttpResponse.ok(updated)
    }

    @Put("/series_to-draft/{seriesUUID}")
    suspend fun setSeriesToDraft(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()
        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        val updated = seriesRegistrationService.setSeriesToDraftStatus(seriesToUpdate, authentication)

        LOG.info("set series to draft: $seriesUUID")
        return HttpResponse.ok(updated)
    }

    @Put("/series-to-inactive/{seriesUUID}")
    suspend fun setPublishedSeriesToInactive(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()
        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }
        val updated =
            seriesRegistrationService.setPublishedSeriesRegistrationStatus(
                seriesToUpdate,
                authentication,
                SeriesStatus.INACTIVE,
            )

        LOG.info("set series to expired: $seriesUUID")
        return HttpResponse.ok(updated)
    }

    @Put("/series-to-active/{seriesUUID}")
    suspend fun setPublishedSeriesToActive(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()
        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }
        val updated =
            seriesRegistrationService.setPublishedSeriesRegistrationStatus(
                seriesToUpdate,
                authentication,
                SeriesStatus.ACTIVE,
            )

        LOG.info("set series to active: $seriesUUID")
        return HttpResponse.ok(updated)
    }

    @Delete("/{seriesUUID}")
    suspend fun deleteSeries(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()

        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        if (seriesToUpdate.draftStatus != DraftStatus.DRAFT) throw BadRequestException("series is not a draft")
        if (seriesToUpdate.published != null) throw BadRequestException("can not delete a published series")

        val updated = seriesRegistrationService.deleteSeries(seriesToUpdate, authentication)

        LOG.info("set series to deleted: $seriesUUID")
        return HttpResponse.ok(updated)
    }

    private fun buildCriteriaSpec(
        inputparams: HashMap<String, String>?,
        supplierId: UUID,
    ): PredicateSpecification<SeriesRegistration>? =
        inputparams?.let {
            PredicateSpecification<SeriesRegistration> { root, criteriaBuilder ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(criteriaBuilder.equal(root[SeriesRegistration::supplierId], supplierId))

                if (inputparams.contains("adminStatus")) {
                    val statusList: List<AdminStatus> =
                        inputparams["adminStatus"]!!.split(",").mapNotNull {
                            try {
                                AdminStatus.valueOf(it)
                            } catch (e: IllegalArgumentException) {
                                throw BadRequestException("Invalid adminStatus: $it")
                            }
                        }
                    if (statusList.isNotEmpty()) {
                        predicates.add(root[SeriesRegistration::adminStatus].`in`(statusList))
                    }
                }

                if (inputparams.contains("excludedStatus")) {
                    predicates.add(
                        criteriaBuilder.notEqual(
                            root[SeriesRegistration::status],
                            inputparams["excludedStatus"]
                        )
                    )
                }

                if (inputparams.contains("status")) {
                    val statusList: List<SeriesStatus> =
                        inputparams["status"]!!.split(",").mapNotNull {
                            try {
                                SeriesStatus.valueOf(it)
                            } catch (e: IllegalArgumentException) {
                                throw BadRequestException("Invalid adminStatus: $it")
                            }
                        }
                    if (statusList.isNotEmpty()) {
                        predicates.add(root[SeriesRegistration::status].`in`(statusList))
                    }
                }
                if (inputparams.contains("supplierId")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::supplierId],
                            UUID.fromString(inputparams["supplierId"]!!)
                        )
                    )
                }
                if (inputparams.contains("draft")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::draftStatus],
                            DraftStatus.valueOf(inputparams["draft"]!!)
                        )
                    )
                }
                if (inputparams.contains("createdByUser")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::createdByUser],
                            inputparams["createdByUser"]
                        )
                    )
                }
                if (inputparams.contains("updatedByUser")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::updatedByUser],
                            inputparams["updatedByUser"]
                        )
                    )
                }

                if (inputparams.contains("editStatus")) {
                    val statusList: List<EditStatus> =
                        inputparams["editStatus"]!!.split(",").map { EditStatus.valueOf(it) }
                    val statusPredicates =
                        statusList.map { status ->
                            when (status) {
                                EditStatus.REJECTED ->
                                    criteriaBuilder.equal(root[SeriesRegistration::adminStatus], AdminStatus.REJECTED)

                                EditStatus.PENDING_APPROVAL ->
                                    criteriaBuilder.and(
                                        criteriaBuilder.equal(root[SeriesRegistration::draftStatus], DraftStatus.DONE),
                                        criteriaBuilder.equal(
                                            root[SeriesRegistration::adminStatus],
                                            AdminStatus.PENDING,
                                        ),
                                    )

                                EditStatus.EDITABLE ->
                                    criteriaBuilder.and(
                                        criteriaBuilder.equal(root[SeriesRegistration::draftStatus], DraftStatus.DRAFT),
                                        criteriaBuilder.equal(
                                            root[SeriesRegistration::adminStatus],
                                            AdminStatus.PENDING,
                                        ),
                                    )

                                EditStatus.DONE ->
                                    criteriaBuilder.equal(root[SeriesRegistration::adminStatus], AdminStatus.APPROVED)
                            }
                        }
                    if (statusPredicates.isNotEmpty()) {
                        predicates.add(criteriaBuilder.or(*statusPredicates.toTypedArray()))
                    }
                }

                if (inputparams.contains("title")) {
                    val term = inputparams["title"]!!.lowercase(Locale.getDefault())
                    predicates.add(
                        criteriaBuilder.like(
                            root[SeriesRegistration::titleLowercase],
                            LiteralExpression("%$term%"),
                        )
                    )
                }

                // Return the combined predicates
                if (predicates.isNotEmpty()) {
                    criteriaBuilder.and(*predicates.toTypedArray())
                } else {
                    null
                }
            }
        }
}


data class SeriesDraftWithDTO(val title: String, val isoCategory: String)
