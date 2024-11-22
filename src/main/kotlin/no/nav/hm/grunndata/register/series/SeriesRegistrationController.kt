package no.nav.hm.grunndata.register.series

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.RequestBean
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.criteria.Predicate
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.mapSuspend
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@Secured(Roles.ROLE_SUPPLIER)
@Controller(SeriesRegistrationController.API_V1_SERIES)
@Tag(name = "Vendor Series")
class SeriesRegistrationController(
    private val seriesRegistrationService: SeriesRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesDTOMapper: SeriesDTOMapper,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationController::class.java)
        const val API_V1_SERIES = "/vendor/api/v1/series"
    }

    @Get("/hmsNr/{hmsNr}")
    suspend fun findSeriesForHmsNr(
        @PathVariable hmsNr: String,
        authentication: Authentication,
    ): SeriesRegistrationDTO? =
        productRegistrationService.findByHmsArtNrAndSupplierId(hmsNr, authentication.supplierId())?.let {
            seriesRegistrationService.findById(it.seriesUUID)?.toDTO()
        }

    @Get("/supplierRef/{supplierRef}")
    suspend fun findSeriesForSupplierRef(
        @PathVariable supplierRef: String,
        authentication: Authentication,
    ): SeriesRegistrationDTO? =
        productRegistrationService.findBySupplierRefAndSupplierIdAndStatusNotDeleted(
            supplierRef,
            authentication.supplierId(),
        )?.let {
            seriesRegistrationService.findById(it.seriesUUID)?.toDTO()
        }

    @Get("/")
    suspend fun getSeries(
        @RequestBean seriesCriteria: SeriesCriteria,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<SeriesRegistrationDTO> {
        return seriesRegistrationService.findAll(buildCriteriaSpec(seriesCriteria, authentication.supplierId()), pageable).mapSuspend { it.toDTO() }
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
        return HttpResponse.ok(seriesRegistrationService.createDraft(authentication.supplierId(), authentication).toDTO())
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
            ).toDTO(),
        )
    }

    @Get("/{id}")
    suspend fun readSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        seriesRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())?.let {
            HttpResponse.ok(it.toDTO())
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
            HttpResponse.ok(seriesDTOMapper.toDTOV2(it))
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

    @Patch("/v2/{id}")
    suspend fun patchSeriesV2(
        @PathVariable id: UUID,
        @Body updateSeriesRegistrationDTO: UpdateSeriesRegistrationDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTOV2> =
        HttpResponse.ok(
            seriesDTOMapper.toDTOV2(
                seriesRegistrationService.patchSeries(
                    id,
                    updateSeriesRegistrationDTO,
                    authentication,
                ),
            ),
        )

    @Post(
        value = "/uploadMedia/{seriesUUID}",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun uploadMedia(
        seriesUUID: UUID,
        files: Publisher<CompletedFileUpload>, // FileUpload-struktur, fra front
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()

        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        LOG.info("supplier: ${authentication.supplierId()} uploading files for series $seriesUUID")
        seriesRegistrationService.uploadMediaAndUpdateSeries(seriesToUpdate, files)

        return HttpResponse.ok()
    }

    @Put("/update-media-priority/{seriesUUID}")
    suspend fun updateMedia(
        seriesUUID: UUID,
        @Body mediaSort: List<MediaSort>,
        authentication: Authentication
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()

        if (seriesToUpdate.supplierId != authentication.supplierId()) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        seriesRegistrationService.updateSeriesMediaPriority(seriesToUpdate, mediaSort, authentication)

        return HttpResponse.ok()
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
        return HttpResponse.ok(updated.toDTO())
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

        return HttpResponse.ok(updated.toDTO())
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

        return HttpResponse.ok(updated.toDTO())
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
        return HttpResponse.ok(updated.toDTO())
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
        return HttpResponse.ok(updated.toDTO())
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
        return HttpResponse.ok(updated.toDTO())
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
        return HttpResponse.ok(updated.toDTO())
    }

    private fun buildCriteriaSpec(
        criteria: SeriesCriteria,
        supplierId: UUID,
    ): PredicateSpecification<SeriesRegistration>? =
        if (criteria.isNotEmpty()) {
            PredicateSpecification<SeriesRegistration> { root, criteriaBuilder ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(criteriaBuilder.equal(root[SeriesRegistration::supplierId], supplierId))


                if (criteria.mainProduct != null) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::mainProduct],
                            criteria.mainProduct,
                        ),
                    )
                }

                if (!criteria.adminStatus.isNullOrEmpty()) {
                    predicates.add(root[SeriesRegistration::adminStatus].`in`(criteria.adminStatus))
                }

                if (criteria.excludeExpired != null  && criteria.excludeExpired) {
                    predicates.add(
                        criteriaBuilder.greaterThan(
                            root[SeriesRegistration::expired],
                            LocalDateTime.now(),
                        ),
                    )
                }

                if (criteria.excludedStatus != null) {
                    predicates.add(
                        criteriaBuilder.notEqual(
                            root[SeriesRegistration::status],
                            criteria.excludedStatus,
                        ),
                    )
                }

                if (!criteria.status.isNullOrEmpty()) {
                    predicates.add(root[SeriesRegistration::status].`in`(criteria.status))
                }

                if (criteria.supplierId != null) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::supplierId],
                            criteria.supplierId)
                    )
                }

                if (criteria.draft != null) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::draftStatus],
                            criteria.draft)
                    )
                }
                if (criteria.createdByUser != null) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::createdByUser],
                            criteria.createdByUser,
                        ),
                    )
                }
                if (criteria.updatedByUser != null) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::updatedByUser],
                            criteria.updatedByUser,
                        ),
                    )
                }
                if (!criteria.editStatus.isNullOrEmpty()) {
                    val statusPredicates = criteria.editStatus.map { status ->
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

                if (criteria.title != null) {
                    val term = criteria.title.lowercase(Locale.getDefault())
                    predicates.add(
                        criteriaBuilder.like(
                            criteriaBuilder.lower(root[SeriesRegistration::title]),
                            LiteralExpression("%$term%"),
                        ),
                    )
                }

                // Return the combined predicates
                if (predicates.isNotEmpty()) {
                    criteriaBuilder.and(*predicates.toTypedArray())
                } else {
                    null
                }
            }
        } else null
}

@Introspected
data class SeriesCriteria (
    val mainProduct: Boolean? = null,
    val adminStatus: List<AdminStatus>? = null,
    val excludeExpired: Boolean? = null,
    val excludedStatus: String? = null,
    val status: List<SeriesStatus>? = null,
    val supplierId: UUID? = null,
    val draft: DraftStatus? = null,
    val createdByUser: String? = null,
    val updatedByUser: String? = null,
    val editStatus: List<EditStatus>? = null,
    val title: String? = null) {
    fun isNotEmpty(): Boolean = mainProduct != null || adminStatus != null || excludeExpired != null ||
            excludedStatus != null || status != null || supplierId != null || draft != null || createdByUser != null
            || updatedByUser != null || editStatus != null || title != null
}

data class SeriesDraftWithDTO(val title: String, val isoCategory: String)
