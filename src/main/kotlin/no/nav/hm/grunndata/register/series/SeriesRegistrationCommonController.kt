package no.nav.hm.grunndata.register.series

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
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
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.isSupplier
import no.nav.hm.grunndata.register.product.mapSuspend
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_ADMIN, Roles.ROLE_SUPPLIER, Roles.ROLE_HMS)
@Controller(SeriesRegistrationCommonController.API_V1_SERIES)
@Tag(name = "Series")
class SeriesRegistrationCommonController(
    private val seriesRegistrationService: SeriesRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesDTOMapper: SeriesDTOMapper,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationAdminController::class.java)
        const val API_V1_SERIES = "/api/v1/series"
    }

    @Post("/supplier/{supplierId}/draftWith")
    suspend fun draftSeriesWith(
        supplierId: UUID,
        @Body draftWith: SeriesDraftWithDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesDraftResponse> {
        if (authentication.isSupplier() && authentication.supplierId() != supplierId) {
            LOG.warn("SupplierId in request does not match authenticated supplierId")
            return HttpResponse.unauthorized()
        }

        return HttpResponse.ok(
            SeriesDraftResponse(
                seriesRegistrationService.createDraftWith(
                    supplierId,
                    authentication,
                    draftWith,
                ).id
            ),
        )
    }

    @Get("/")
    suspend fun findSeries(
        @RequestBean seriesCriteria: SeriesCommonCriteria,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<SeriesSearchDTO> {
        var spec = buildCriteriaSpec(seriesCriteria, authentication)

        // Collect all ID-based filters and combine them efficiently to avoid SQL parameter limit
        var filteredIds: Set<UUID>? = null
        var seriesIdsInAgreement: Set<UUID>? = null

        if (seriesCriteria.inAgreement != null) {
            seriesIdsInAgreement = seriesRegistrationService.findSeriesIdsOnAgreement().toSet()
            if (seriesCriteria.inAgreement) {
                filteredIds = seriesIdsInAgreement
            }
            // If inAgreement=false, we'll subtract these IDs from filteredIds later
        }

        if (seriesCriteria.missingMediaType != null) {
            val seriesIdsWithMissingMedia = seriesRegistrationService.findSeriesIdsWithMissingMediaType(
                seriesCriteria.missingMediaType
            ).toSet()

            filteredIds = when {
                filteredIds == null -> seriesIdsWithMissingMedia
                else -> filteredIds.intersect(seriesIdsWithMissingMedia) // Intersection reduces parameter count
            }
        }

        // Apply "not in agreement" by subtracting IDs in application code (avoids large NOT IN clause)
        if (seriesCriteria.inAgreement == false && seriesIdsInAgreement != null) {
            filteredIds = when {
                filteredIds == null -> {
                    // No other filters, we need to fetch all series IDs and subtract agreement IDs
                    // This is expensive, so we'll use NOT IN clause in this specific case
                    null
                }

                else -> {
                    // Subtract agreement IDs from the filtered set
                    filteredIds.minus(seriesIdsInAgreement)
                }
            }
        }

        // Create a single predicate based on the combined filtered IDs
        if (filteredIds != null) {
            val idsSpec = if (filteredIds.isEmpty()) {
                PredicateSpecification<SeriesRegistration> { _, criteriaBuilder ->
                    criteriaBuilder.disjunction() // No results
                }
            } else {
                PredicateSpecification<SeriesRegistration> { root, _ ->
                    root[SeriesRegistration::id].`in`(filteredIds)
                }
            }
            spec = if (spec != null) {
                spec.and(idsSpec)
            } else {
                idsSpec
            }
        } else if (seriesCriteria.inAgreement == false && seriesIdsInAgreement != null) {
            // Only use NOT IN when there are no other ID-based filters
            val notInAgreementSpec = if (seriesIdsInAgreement.isEmpty()) {
                PredicateSpecification<SeriesRegistration> { _, criteriaBuilder ->
                    criteriaBuilder.conjunction() // All results
                }
            } else {
                PredicateSpecification<SeriesRegistration> { root, criteriaBuilder ->
                    criteriaBuilder.not(root[SeriesRegistration::id].`in`(seriesIdsInAgreement))
                }
            }
            spec = if (spec != null) {
                spec.and(notInAgreementSpec)
            } else {
                notInAgreementSpec
            }
        }

        return seriesRegistrationService.findAll(spec, pageable)
            .mapSuspend { it.toSearchDTO() }
    }

    @Get("/variant-id/{variantIdentifier}")
    suspend fun findSeriesByVariantIdentifier(
        @PathVariable variantIdentifier: String,
        authentication: Authentication,
    ): SeriesSearchDTO? {
        val variant = productRegistrationService.findByHmsArtNr(variantIdentifier, authentication)
            ?: productRegistrationService.findBySupplierRef(variantIdentifier, authentication)

        return variant?.let { seriesRegistrationService.findById(it.seriesUUID)?.toSearchDTO() }
    }

    private fun SeriesRegistration.toSearchDTO() = SeriesSearchDTO(
        id = id,
        title = title,
        status = EditStatus.from(this),
        updated = updated,
        updatedByUser = updatedByUser,
        thumbnail = seriesData.media.sortedBy { it.priority }.firstOrNull { it.type == MediaType.IMAGE },
        isExpired = expired < LocalDateTime.now(),
        isPublished = published?.isBefore(LocalDateTime.now()) ?: false,
        variantCount = count,
        mainProduct = mainProduct,
    )

    @Get("/{id}")
    suspend fun readSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesDTO> {
        return seriesRegistrationService.findById(id, authentication)?.let {
            HttpResponse.ok(seriesDTOMapper.toDTOV2(it))
        } ?: run {
            LOG.warn("Series with id $id does not exist")
            HttpResponse.notFound()
        }
    }

    @Patch("/{id}")
    suspend fun patchSeries(
        @PathVariable id: UUID,
        @Body updateSeriesRegistrationDTO: UpdateSeriesRegistrationDTO,
        authentication: Authentication,
    ): HttpResponse<Any> {
        seriesRegistrationService.patchSeries(
            id,
            updateSeriesRegistrationDTO,
            authentication,
        )
        return HttpResponse.ok()
    }

    @Put("/series_to-draft/{id}")
    suspend fun setSeriesToDraft(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(id, authentication) ?: return HttpResponse.notFound()

        seriesRegistrationService.setSeriesToDraftStatus(seriesToUpdate, authentication)

        LOG.info("set series to draft: $id")
        return HttpResponse.ok()
    }

    @Put("/series-to-inactive/{id}")
    suspend fun setPublishedSeriesToInactive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(id, authentication) ?: return HttpResponse.notFound()

        seriesRegistrationService.setPublishedSeriesRegistrationStatus(
            seriesToUpdate,
            authentication,
            SeriesStatus.INACTIVE,
        )

        LOG.info("set series to expired: $id")
        return HttpResponse.ok()
    }

    @Put("/series-to-active/{id}")
    suspend fun setPublishedSeriesToActive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(id, authentication) ?: return HttpResponse.notFound()

        seriesRegistrationService.setPublishedSeriesRegistrationStatus(
            seriesToUpdate,
            authentication,
            SeriesStatus.ACTIVE,
        )

        LOG.info("set series to active: $id")
        return HttpResponse.ok()
    }

    @Delete("/{id}")
    suspend fun deleteSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(id, authentication) ?: return HttpResponse.notFound()

        if (authentication.isSupplier()) {
            if (seriesToUpdate.draftStatus != DraftStatus.DRAFT) throw BadRequestException("series is not a draft")
            if (seriesToUpdate.published != null) throw BadRequestException("can not delete a published series")
        }

        seriesRegistrationService.deleteSeries(seriesToUpdate, authentication)
        LOG.info("set series to deleted: $id")
        return HttpResponse.ok()
    }

    @Delete("/draft/{id}")
    suspend fun deleteDraftSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(id, authentication) ?: return HttpResponse.notFound()

        if (authentication.isSupplier()) {
            if (seriesToUpdate.draftStatus != DraftStatus.DRAFT) throw BadRequestException("series is not a draft")
            if (seriesToUpdate.published != null) throw BadRequestException("can not delete a published series")
        }

        seriesRegistrationService.deleteDraft(seriesToUpdate, authentication)
        LOG.info("set series to deleted: $id")
        return HttpResponse.ok()
    }

    @Post(
        value = "/upload-media/{seriesUUID}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun uploadMedia(
        seriesUUID: UUID,
        files: Publisher<CompletedFileUpload>, // FileUpload-struktur, fra front
        authentication: Authentication,
    ): HttpResponse<Any> {

        LOG.info("Uploading files for series $seriesUUID")
        seriesRegistrationService.uploadMediaAndUpdateSeries(seriesUUID, files, authentication)

        return HttpResponse.ok()
    }

    @Delete("/delete-media/{seriesUUID}")
    suspend fun deleteMedia(
        seriesUUID: UUID,
        @Body mediaUris: List<String>,
        authentication: Authentication
    ): HttpResponse<Any> {
        seriesRegistrationService.deleteSeriesMedia(seriesUUID, mediaUris, authentication)
        return HttpResponse.ok()
    }

    @Put("/update-media-priority/{seriesUUID}")
    suspend fun updateMedia(
        seriesUUID: UUID,
        @Body mediaSort: List<MediaSort>,
        authentication: Authentication
    ): HttpResponse<Any> {
        seriesRegistrationService.updateSeriesMediaPriority(seriesUUID, mediaSort, authentication)

        return HttpResponse.ok()
    }

    @Put("/add-document-url/{seriesUUID}")
    suspend fun addDocumentUrlToSeries(
        seriesUUID: UUID,
        @Body documentUrl: NewDocumentUrl,
        authentication: Authentication
    ): HttpResponse<Any> {
        seriesRegistrationService.addDocumentUrl(seriesUUID, documentUrl, authentication)
        return HttpResponse.ok()
    }

    @Delete("/delete-document-url/{seriesUUID}")
    suspend fun deleteDocumentUrl(
        seriesUUID: UUID,
        @Body documentUrl: DeleteDocumentUrl,
        authentication: Authentication
    ): HttpResponse<Any> {
        seriesRegistrationService.deleteDocumentUrl(seriesUUID, documentUrl.uri, authentication)
        return HttpResponse.ok()
    }

    @Put("/add-videos/{seriesUUID}")
    suspend fun addVideoToSeries(
        seriesUUID: UUID,
        @Body videos: List<NewVideo>,
        authentication: Authentication
    ): HttpResponse<Any> {
        seriesRegistrationService.addVideos(seriesUUID, videos, authentication)
        return HttpResponse.ok()
    }

    @Put("/change-file-title/{seriesUUID}")
    suspend fun changeFileTitle(
        seriesUUID: UUID,
        @Body file: FileTitleDto,
        authentication: Authentication
    ): HttpResponse<Any> {
        LOG.info("endre filtittel")
        seriesRegistrationService.changeFileTitle(seriesUUID, file, authentication)
        return HttpResponse.ok()
    }

    private fun buildCriteriaSpec(
        criteria: SeriesCommonCriteria, authentication: Authentication
    ): PredicateSpecification<SeriesRegistration>? = if (criteria.isNotEmpty()) {
        PredicateSpecification<SeriesRegistration> { root, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            if (authentication.isSupplier()) {
                predicates.add(
                    criteriaBuilder.equal(
                        root[SeriesRegistration::supplierId], authentication.supplierId()
                    )
                )
            }

            if (criteria.mainProduct != null) {
                predicates.add(
                    criteriaBuilder.equal(
                        root[SeriesRegistration::mainProduct],
                        criteria.mainProduct,
                    ),
                )
            }

            if (criteria.adminStatus != null) {
                predicates.add(
                    criteriaBuilder.equal(
                        root[SeriesRegistration::adminStatus],
                        criteria.adminStatus,
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
            if (criteria.excludeExpired != null && criteria.excludeExpired) {
                predicates.add(
                    criteriaBuilder.greaterThan(
                        root[SeriesRegistration::expired],
                        LocalDateTime.now(),
                    ),
                )
            }
            if (!criteria.status.isNullOrEmpty()) {
                predicates.add(root[SeriesRegistration::status].`in`(criteria.status))
            }

            if (criteria.supplierId != null) {
                predicates.add(
                    criteriaBuilder.equal(
                        root[SeriesRegistration::supplierId], criteria.supplierId
                    )
                )
            }
            if (criteria.draft != null) {
                predicates.add(
                    criteriaBuilder.equal(
                        root[SeriesRegistration::draftStatus], criteria.draft
                    )
                )
            }
            if (criteria.createdByUser != null) {
                predicates.add(
                    criteriaBuilder.equal(
                        root[SeriesRegistration::createdByUser], criteria.createdByUser
                    )
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
            if (criteria.createdByAdmin != null) {
                predicates.add(
                    criteriaBuilder.equal(
                        root[SeriesRegistration::createdByAdmin],
                        criteria.createdByAdmin,
                    ),
                )
            }

            if (criteria.supplierFilter != null) {
                predicates.add(root[SeriesRegistration::supplierId].`in`(criteria.supplierFilter))
            }

            if (criteria.editStatus != null) {
                val statusPredicates = criteria.editStatus.map { status ->
                    when (status) {
                        EditStatus.REJECTED -> criteriaBuilder.equal(
                            root[SeriesRegistration::adminStatus],
                            AdminStatus.REJECTED,
                        )

                        EditStatus.PENDING_APPROVAL -> criteriaBuilder.and(
                            criteriaBuilder.equal(
                                root[SeriesRegistration::draftStatus],
                                DraftStatus.DONE,
                            ),
                            criteriaBuilder.equal(
                                root[SeriesRegistration::adminStatus],
                                AdminStatus.PENDING,
                            ),
                        )

                        EditStatus.EDITABLE -> criteriaBuilder.and(
                            criteriaBuilder.equal(
                                root[SeriesRegistration::draftStatus],
                                DraftStatus.DRAFT,
                            ),
                            criteriaBuilder.equal(
                                root[SeriesRegistration::adminStatus],
                                AdminStatus.PENDING,
                            ),
                        )

                        EditStatus.DONE -> criteriaBuilder.equal(
                            root[SeriesRegistration::adminStatus],
                            AdminStatus.APPROVED,
                        )
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
data class SeriesCommonCriteria(
    val mainProduct: Boolean? = null,
    val adminStatus: AdminStatus? = null,
    val excludedStatus: SeriesStatus? = null,
    val excludeExpired: Boolean? = null,
    val status: List<SeriesStatus>? = null,
    val supplierId: UUID? = null,
    val draft: DraftStatus? = null,
    val createdByUser: String? = null,
    val updatedByUser: String? = null,
    val createdByAdmin: Boolean? = null,
    val supplierFilter: List<UUID>? = null,
    val editStatus: List<EditStatus>? = null,
    val title: String? = null,
    val inAgreement: Boolean? = null,
    val missingMediaType: String? = null,
) {
    fun isNotEmpty(): Boolean =
        mainProduct != null || adminStatus != null || excludedStatus != null || excludeExpired != null
                || status != null || supplierId != null || draft != null || createdByUser != null
                || updatedByUser != null || createdByAdmin != null || supplierFilter != null || editStatus != null
                || title != null || inAgreement != null || missingMediaType != null
}