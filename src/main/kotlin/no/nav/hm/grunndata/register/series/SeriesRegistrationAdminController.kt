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
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.annotation.RequestBean
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

    @Get("/")
    suspend fun getSeries(
        @RequestBean seriesCriteria: SeriesAdminCriteria,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<SeriesRegistrationDTO> {
        return seriesRegistrationService.findAll(buildCriteriaSpec(seriesCriteria), pageable).mapSuspend { it.toDTO() }
    }

    private fun buildCriteriaSpec(criteria: SeriesAdminCriteria): PredicateSpecification<SeriesRegistration>? =
        if (criteria.isNotEmpty()) {
            PredicateSpecification<SeriesRegistration> { root, criteriaBuilder ->
                val predicates = mutableListOf<Predicate>()

                if (criteria.isPublished != null) {
                    if (criteria.isPublished) {
                        predicates.add(
                            criteriaBuilder.isNotNull(
                                root[SeriesRegistration::published]
                            )
                        )
                    } else {
                        predicates.add(
                            criteriaBuilder.isNull(
                                root[SeriesRegistration::published]
                            )
                        )
                    }
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
                            root[SeriesRegistration::supplierId],
                            criteria.supplierId
                        )
                    )
                }
                if (criteria.draft != null) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::draftStatus],
                            criteria.draft
                        )
                    )
                }
                if (criteria.createdByUser != null) {
                    predicates.add(
                        criteriaBuilder.equal(
                            root[SeriesRegistration::createdByUser],
                            criteria.createdByUser
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
                    val statusPredicates =
                        criteria.editStatus.map { status ->
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

    @Get("/{id}")
    suspend fun readSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> =
        seriesRegistrationService.findById(id)?.let {
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

    @Get("/to-approve{?params*}")
    suspend fun findSeriesPendingApprove(
        @QueryValue params: java.util.HashMap<String, String>?,
        pageable: Pageable,
    ): Page<SeriesToApproveDTO> = seriesRegistrationService.findSeriesToApprove(pageable, params)

    @Put("/approve-v2/{id}")
    suspend fun approveSeriesAndVariants(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(id) ?: return HttpResponse.notFound()

        if (seriesToUpdate.adminStatus == AdminStatus.APPROVED) throw BadRequestException("$id is already approved")
        if (seriesToUpdate.status == SeriesStatus.DELETED) throw BadRequestException("SeriesStatus should not be Deleted")

        seriesRegistrationService.approveSeriesAndVariants(seriesToUpdate, authentication)

        LOG.info("set series to approved: $id")
        return HttpResponse.ok()
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

        return HttpResponse.ok(updatedSeries.map { it.toDTO() })
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
                ).toDTO()
            HttpResponse.ok(dto)
        } ?: HttpResponse.notFound()

    @Put("/reject-v2/{id}")
    suspend fun rejectSeriesAndVariants(
        id: UUID,
        @Body rejectSeriesDTO: RejectSeriesDTO,
        authentication: Authentication,
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findById(id) ?: return HttpResponse.notFound()
        if (seriesToUpdate.adminStatus != AdminStatus.PENDING) throw BadRequestException("series is not pending approval")
        if (seriesToUpdate.draftStatus != DraftStatus.DONE) throw BadRequestException("series is not done")

        seriesRegistrationService.rejectSeriesAndVariants(seriesToUpdate, rejectSeriesDTO.message, authentication)

        LOG.info("set series to rejected: $id")
        return HttpResponse.ok()
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

    @Post("/supplier/{supplierId}/draftWith")
    suspend fun draftSeriesWith(
        supplierId: UUID,
        @Body draftWith: SeriesDraftWithDTO,
        authentication: Authentication,
    ): HttpResponse<SeriesDraftResponse> {
        return HttpResponse.ok(
            SeriesDraftResponse(seriesRegistrationService.createDraftWith(
                supplierId,
                authentication,
                draftWith,
            ).id),
        )
    }
}

@Introspected
data class SeriesAdminCriteria(
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
    val isPublished: Boolean? = null
) {
    fun isNotEmpty(): Boolean =
        mainProduct != null || adminStatus != null || excludedStatus != null || excludeExpired != null
                || status != null || supplierId != null || draft != null || createdByUser != null
                || updatedByUser != null || createdByAdmin != null || supplierFilter != null || editStatus != null
                || title != null || isPublished != null
}

data class RejectSeriesDTO(val message: String?)

data class SupplierInventoryDTO(val numberOfSeries: Int, val numberOfVariants: Int)
