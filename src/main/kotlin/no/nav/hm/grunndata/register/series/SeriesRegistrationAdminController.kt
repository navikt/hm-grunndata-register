package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
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
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.techlabel.TechLabelService
import org.slf4j.LoggerFactory
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

    @Put("/toPart/{seriesId}")
    suspend fun changeMainProductToPart(
        seriesId: UUID,
        @Body accessoryDTO: AccessoryDTO,
    ): HttpResponse<Any> {
        return when (seriesRegistrationService.changeMainProductToPart(
            seriesId,
            accessoryDTO.accessory,
            accessoryDTO.newIsoCode,
            accessoryDTO.resetTechnicalData,
        )) {
            is SeriesRegistrationService.ChangeToPartResult.Ok -> HttpResponse.ok()
            is SeriesRegistrationService.ChangeToPartResult.AlreadyPart -> {
                LOG.warn("Series $seriesId is already set as part")
                HttpResponse.badRequest("Series $seriesId is already set as part")
            }

            is SeriesRegistrationService.ChangeToPartResult.NotFound -> HttpResponse.notFound()
        }
    }

    @Get("/to-approve{?params*}")
    suspend fun findSeriesPendingApprove(
        @QueryValue params: java.util.HashMap<String, String>?,
        pageable: Pageable,
    ): Page<SeriesToApproveDTO> = seriesRegistrationService.findSeriesToApprove(pageable, params)

    @Get("/to-approve/variant-id/{variantIdentifier}")
    suspend fun findSeriesPendingApproveByVariantIdentifier(
        @PathVariable variantIdentifier: String,
        authentication: Authentication,
    ): SeriesToApproveDTO? {
        val variant = productRegistrationService.findByHmsArtNr(variantIdentifier, authentication)
            ?: productRegistrationService.findBySupplierRef(variantIdentifier, authentication)

        return variant?.let { seriesRegistrationService.findToApproveById(it.seriesUUID) }
    }

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
    ): HttpResponse<Any> {
        val seriesToUpdate = seriesRegistrationService.findByIdIn(ids).onEach {
            if (it.draftStatus != DraftStatus.DONE) throw BadRequestException("product is not done")
            if (it.adminStatus == AdminStatus.APPROVED) throw BadRequestException("${it.id} is already approved")
            if (it.status == SeriesStatus.DELETED) {
                throw BadRequestException(
                    "RegistrationStatus should not be Deleted",
                )
            }
        }

        seriesRegistrationService.approveManySeriesAndVariants(seriesToUpdate, authentication)

        return HttpResponse.ok()
    }

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

    @Put("/series/products/move-to/{seriesId}")
    suspend fun moveProductVariantsToSeries(
        seriesId: UUID,
        productIds: List<UUID>,
        authentication: Authentication,
    ) {
        LOG.info("Moving products to series $seriesId")
        seriesRegistrationService.moveVariantsToSeries(seriesId, productIds, authentication)
    }

    @Put("/move-owner/supplier/{fromSupplierId}/{toSupplierId}")
    suspend fun moveOwnerSupplier(fromSupplierId: UUID, toSupplierId: UUID) {
        val seriesFromSupplier = seriesRegistrationService.findBySupplierId(fromSupplierId)
        LOG.info("Got ${seriesFromSupplier.size} series from supplier $fromSupplierId")
        seriesFromSupplier.forEach { series ->
            seriesRegistrationService.moveOwnerForSeries(series, toSupplierId)
        }
        LOG.info("All series and products from supplier $fromSupplierId moved to supplier $toSupplierId")
    }

    @Post("/ungroup/{seriesId")
    suspend fun ungroupSeies(
        seriesId: UUID,
    ): HttpResponse<Any> {
        val seriesToUngroup = seriesRegistrationService.findById(seriesId) ?: return HttpResponse.notFound()
        seriesRegistrationService.ungroupSeries(seriesToUngroup)
        LOG.info("Ungrouped series: $seriesId")
        return HttpResponse.ok()
    }
}


data class AccessoryDTO(val accessory: Boolean, val newIsoCode: String, val resetTechnicalData: Boolean = false)

data class RejectSeriesDTO(val message: String?)

data class SupplierInventoryDTO(val numberOfSeries: Int, val numberOfVariants: Int)
