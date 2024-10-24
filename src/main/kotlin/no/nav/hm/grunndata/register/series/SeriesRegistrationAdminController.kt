package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.LiteralExpression
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
import io.micronaut.http.annotation.QueryValue
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
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(SeriesRegistrationAdminController.API_V1_SERIES)
@Tag(name = "Admin Series")
class SeriesRegistrationAdminController(
    private val seriesRegistrationService: SeriesRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val seriesDTOMapper: SeriesDTOMapper,
) {
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

    @Get("/hmsNr/{hmsNr}")
    suspend fun findSeriesForHmsNr(
        @PathVariable hmsNr: String,
        authentication: Authentication,
    ): SeriesRegistrationDTO? =
        productRegistrationService.findByHmsArtNr(hmsNr)?.let {
            seriesRegistrationService.findById(it.seriesUUID)
        }

    @Get("/supplierRef/{supplierRef}")
    suspend fun findSeriesForSupplierRef(
        @PathVariable supplierRef: String,
        authentication: Authentication,
    ): SeriesRegistrationDTO? =
        productRegistrationService.findBySupplierRef(supplierRef)?.let {
            seriesRegistrationService.findById(it.seriesUUID)
        }

    private fun buildCriteriaSpec(inputparams: java.util.HashMap<String, String>?): PredicateSpecification<SeriesRegistration>? =
        inputparams?.let {
            PredicateSpecification<SeriesRegistration> { root, criteriaBuilder ->
                val predicates = mutableListOf<Predicate>()

                if (inputparams.contains("mainProduct")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::mainProduct],
                            inputparams["mainProduct"],
                        ),
                    )
                }

                if (inputparams.contains("adminStatus")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::adminStatus],
                            AdminStatus.valueOf(inputparams["adminStatus"]!!),
                        ),
                    )
                }
                if (inputparams.contains("excludedStatus")) {
                    predicates.add(
                        criteriaBuilder.notEqual(
                            root[SeriesRegistration::status],
                            inputparams["excludedStatus"],
                        ),
                    )
                }
                if (inputparams.contains("excludeExpired") && inputparams["excludeExpired"] == "true") {
                    predicates.add(
                        criteriaBuilder.greaterThan(
                            root[SeriesRegistration::expired],
                            LocalDateTime.now(),
                        ),
                    )
                }
                if (inputparams.contains("status")) {
                    val statusList: List<SeriesStatus> =
                        inputparams["status"]!!.split(",").map { SeriesStatus.valueOf(it) }
                    predicates.add(root[SeriesRegistration::status].`in`(statusList))
                }

                if (inputparams.contains("supplierId")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::supplierId],
                            UUID.fromString(inputparams["supplierId"]!!),
                        ),
                    )
                }
                if (inputparams.contains("draft")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::draftStatus],
                            DraftStatus.valueOf(inputparams["draft"]!!),
                        ),
                    )
                }
                if (inputparams.contains("createdByUser")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::createdByUser],
                            inputparams["createdByUser"],
                        ),
                    )
                }
                if (inputparams.contains("updatedByUser")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::updatedByUser],
                            inputparams["updatedByUser"],
                        ),
                    )
                }

                if (inputparams.contains("createdByAdmin")) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::createdByAdmin],
                            it["createdByAdmin"].toBoolean(),
                        ),
                    )
                }

                if (inputparams.contains("supplierFilter")) {
                    val supplierList: List<UUID> =
                        inputparams["supplierFilter"]!!.split(",").map { UUID.fromString(it) }
                    predicates.add(root[SeriesRegistration::supplierId].`in`(supplierList))
                }

                if (inputparams.contains("editStatus")) {
                    val statusList: List<EditStatus> =
                        inputparams["editStatus"]!!.split(",").map { EditStatus.valueOf(it) }

                    val statusPredicates =
                        statusList.map { status ->
                            when (status) {
                                EditStatus.REJECTED ->
                                    criteriaBuilder.equal(
                                        root[SeriesRegistration::adminStatus],
                                        AdminStatus.REJECTED,
                                    )

                                EditStatus.PENDING_APPROVAL ->
                                    criteriaBuilder.and(
                                        criteriaBuilder.equal(
                                            root[SeriesRegistration::draftStatus],
                                            DraftStatus.DONE,
                                        ),
                                        criteriaBuilder.equal(
                                            root[SeriesRegistration::adminStatus],
                                            AdminStatus.PENDING,
                                        ),
                                    )

                                EditStatus.EDITABLE ->
                                    criteriaBuilder.and(
                                        criteriaBuilder.equal(
                                            root[SeriesRegistration::draftStatus],
                                            DraftStatus.DRAFT,
                                        ),
                                        criteriaBuilder.equal(
                                            root[SeriesRegistration::adminStatus],
                                            AdminStatus.PENDING,
                                        ),
                                    )

                                EditStatus.DONE ->
                                    criteriaBuilder.equal(
                                        root[SeriesRegistration::adminStatus],
                                        AdminStatus.APPROVED,
                                    )
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

    @Get("/v2/{id}")
    suspend fun readSeriesV2(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTOV2> {
        return seriesRegistrationService.findByIdV2(id)?.let {
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

    @Put("/v2/{id}")
    suspend fun updateSeriesV2(
        @PathVariable id: UUID,
        @Body updateSeriesRegistrationDTO: UpdateSeriesRegistrationDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        seriesRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())?.let { inDb ->
            HttpResponse.ok(
                seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                    inDb
                        .copy(
                            title = updateSeriesRegistrationDTO.title ?: inDb.title,
                            text = updateSeriesRegistrationDTO.text ?: inDb.text,
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

    @Get("/to-approve{?params*}")
    suspend fun findSeriesPendingApprove(
        @QueryValue params: java.util.HashMap<String, String>?,
        pageable: Pageable,
    ): Page<SeriesToApproveDTO> = seriesRegistrationService.findSeriesToApprove(pageable, params)

    @Put("/approve-v2/{id}")
    suspend fun approveSeriesAndVariants(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(id) ?: return HttpResponse.notFound()

        if (seriesToUpdate.adminStatus == AdminStatus.APPROVED) throw BadRequestException("$id is already approved")
        if (seriesToUpdate.status == SeriesStatus.DELETED) throw BadRequestException("SeriesStatus should not be Deleted")

        val updatedSeries = seriesRegistrationService.approveSeriesAndVariants(seriesToUpdate, authentication)

        LOG.info("set series to approved: $id")
        return HttpResponse.ok(updatedSeries)
    }

    @Put("/approve-multiple")
    suspend fun approveSeriesAndVariants(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<SeriesRegistrationDTO>> {
        val seriesToUpdate =
            seriesRegistrationService.findByIdIn(ids).onEach {
                if (it.draftStatus != DraftStatus.DONE) throw BadRequestException("product is not done")
                if (it.adminStatus == AdminStatus.APPROVED) throw BadRequestException("${it.id} is already approved")
                if (it.status == SeriesStatus.DELETED) {
                    throw BadRequestException(
                        "RegistrationStatus should not be Deleted",
                    )
                }
            }

        val updatedSeries = seriesRegistrationService.approveManySeriesAndVariants(seriesToUpdate, authentication)

        return HttpResponse.ok(updatedSeries)
    }

    @Put("/reject/{id}")
    suspend fun rejectSeries(
        id: UUID,
        @Body rejectSeriesDTO: RejectSeriesDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        seriesRegistrationService.findById(id)?.let {
            if (it.adminStatus != AdminStatus.PENDING) throw BadRequestException("series is not pending approval")
            if (it.draftStatus != DraftStatus.DONE) throw BadRequestException("series is not done")
            if (it.status != SeriesStatus.ACTIVE) throw BadRequestException("SeriesStatus should be Active")
            val dto =
                seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                    it.copy(
                        message = rejectSeriesDTO.message,
                        draftStatus = DraftStatus.DRAFT,
                        adminStatus = AdminStatus.REJECTED,
                        updated = LocalDateTime.now(),
                        updatedBy = REGISTER,
                    ),
                    isUpdate = true,
                )
            HttpResponse.ok(dto)
        } ?: HttpResponse.notFound()

    @Put("/reject-v2/{id}")
    suspend fun rejectSeriesAndVariants(
        id: UUID,
        @Body rejectSeriesDTO: RejectSeriesDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(id) ?: return HttpResponse.notFound()
        if (seriesToUpdate.adminStatus != AdminStatus.PENDING) throw BadRequestException("series is not pending approval")
        if (seriesToUpdate.draftStatus != DraftStatus.DONE) throw BadRequestException("series is not done")

        val updatedSeries =
            seriesRegistrationService.rejectSeriesAndVariants(seriesToUpdate, rejectSeriesDTO.message, authentication)

        LOG.info("set series to rejected: $id")
        return HttpResponse.ok(updatedSeries)
    }

    @Delete("/{id}")
    suspend fun deleteSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(id) ?: return HttpResponse.notFound()

        val updated = seriesRegistrationService.deleteSeries(seriesToUpdate, authentication)

        LOG.info("set series to deleted: $id")
        return HttpResponse.ok(updated)
    }

    @Put("/series_to-draft/{seriesUUID}")
    suspend fun setSeriesToDraft(
        @PathVariable seriesUUID: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(seriesUUID) ?: return HttpResponse.notFound()
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
        val updated =
            seriesRegistrationService.setPublishedSeriesRegistrationStatus(
                seriesToUpdate,
                authentication,
                SeriesStatus.ACTIVE,
            )

        LOG.info("set series to active: $seriesUUID")
        return HttpResponse.ok(updated)
    }

    @Get("/supplier-inventory/{id}")
    suspend fun getSupplierProductInfo(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<SupplierInventoryDTO> {
        val numberOfSeries = seriesRegistrationService.countBySupplier(id).toInt()
        val numberOfVariants = productRegistrationService.countBySupplier(id).toInt()

        return HttpResponse.ok(
            SupplierInventoryDTO(
                numberOfSeries = numberOfSeries,
                numberOfVariants = numberOfVariants,
            ),
        )
    }

    @Post("series/create-from/products")
    suspend fun createSeriesFromProductList(
        @Body productIds: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        LOG.info("creating a new series based on a list of productId")
        val seriesId = UUID.randomUUID()
        return HttpResponse.ok(
            productRegistrationService.findById(productIds.first())?.let { product ->
                val series =
                    seriesRegistrationService.save(
                        SeriesRegistrationDTO(
                            id = seriesId,
                            supplierId = product.supplierId,
                            isoCategory = product.isoCategory,
                            title = product.title,
                            text = product.productData.attributes.text ?: "",
                            identifier = seriesId.toString(),
                            draftStatus = DraftStatus.DONE,
                            adminStatus = AdminStatus.PENDING,
                            status = SeriesStatus.ACTIVE,
                            createdBy = REGISTER,
                            updatedBy = REGISTER,
                            createdByAdmin = true,
                            createdByUser = authentication.name,
                            created = LocalDateTime.now(),
                            updated = LocalDateTime.now(),
                            seriesData = SeriesDataDTO(media = product.productData.media),
                            version = 0,
                        ),
                    )
                productIds.forEach { productId ->
                    productRegistrationService.findById(productId)?.let {
                        productRegistrationService.save(
                            it.copy(
                                seriesUUID = series.id,
                                adminStatus = AdminStatus.PENDING,
                                updated = LocalDateTime.now(),
                                updatedByUser = authentication.name,
                            ),
                        )
                    }
                }
                series
            } ?: throw BadRequestException("Product with id ${productIds.first()} does not exist"),
        )
    }

    @Put("/series/products/move-to/{seriesId}")
    suspend fun moveProductVariantsToSeries(
        seriesId: UUID,
        productIds: List<UUID>,
        authentication: Authentication,
    ) {
        LOG.info("Moving products to series $seriesId")
        seriesRegistrationService.moveVariantsToSeries(seriesId, productIds, authentication)
    }

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

        LOG.info("admin uploading files for series $seriesUUID")
        seriesRegistrationService.uploadMediaAndUpdateSeries(seriesToUpdate, files)

        return HttpResponse.ok()
    }

    @Post("/supplier/{supplierId}/draftWith")
    suspend fun draftSeriesWith(
        supplierId: UUID,
        @Body draftWith: SeriesDraftWithDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        return HttpResponse.ok(
            seriesRegistrationService.createDraftWith(
                supplierId,
                authentication,
                draftWith,
            ),
        )
    }
}

data class RejectSeriesDTO(val message: String?)

data class SupplierInventoryDTO(val numberOfSeries: Int, val numberOfVariants: Int)
