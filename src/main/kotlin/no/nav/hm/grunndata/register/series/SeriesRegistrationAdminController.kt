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
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationController.Companion
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
                } else if (params.contains("term")) {
                    val term = params["term"]!!.lowercase(Locale.getDefault())
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

    @Get("/v2/{id}")
    suspend fun readSeriesV2(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTOV2> {
        return seriesRegistrationService.findByIdV2(id)?.let {
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
                            updatedByUser = authentication.name
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
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(id) ?: return HttpResponse.notFound()

        if (seriesToUpdate.adminStatus == AdminStatus.APPROVED) throw BadRequestException("$id is already approved")
        if (seriesToUpdate.draftStatus != DraftStatus.DONE) throw BadRequestException("Series is not done")
        if (seriesToUpdate.status == SeriesStatus.DELETED) throw BadRequestException("SeriesStatus should not be Deleted")

        val updatedSeries = seriesRegistrationService.approveSeriesAndVariants(seriesToUpdate, authentication)

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

    @Delete("/{id}")
    suspend fun deleteSeries(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<SeriesRegistrationDTO> {
        val seriesToUpdate = seriesRegistrationService.findById(id) ?: return HttpResponse.notFound()

        val updated = seriesRegistrationService.deleteSeries(seriesToUpdate, authentication)

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
        seriesRegistrationService.findById(seriesId)
            ?: throw BadRequestException("Series with id $seriesId does not exist")
        productIds.forEach { productId ->
            productRegistrationService.findById(productId)?.let {
                productRegistrationService.save(
                    it.copy(
                        seriesUUID = seriesId,
                        adminStatus = AdminStatus.PENDING,
                        updatedByUser = authentication.name,
                        updated = LocalDateTime.now(),
                    ),
                )
            }
        }
    }
}

data class RejectSeriesDTO(val message: String?)

data class SupplierInventoryDTO(val numberOfSeries: Int, val numberOfVariants: Int)
