package no.nav.hm.grunndata.register.part

import io.micronaut.security.authentication.Authentication
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.isSupplier
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

@Singleton
open class PartService(
    private val seriesService: SeriesRegistrationService,
    private val productService: ProductRegistrationService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(PartService::class.java)
    }

    @Transactional
    open suspend fun createDraftWith(auth: Authentication, draft: PartDraftWithDTO): ProductRegistration {
        val series = draft.toSeriesRegistration(auth).also { seriesService.save(it) }
        val product = draft.toProductRegistration(series.id).also { productService.save(it) }
        LOG.info("Created part draft for series: ${series.id}, product: ${product.id}")
        return product
    }

    @Transactional
    open suspend fun createDraftWithAndApprove(auth: Authentication, draft: PartDraftWithDTO): ProductRegistration {
        val series = draft.toSeriesRegistration(auth).also { seriesService.save(it) }
        val product = draft.toProductRegistration(series.id).also { productService.save(it) }
        seriesService.approveSeriesAndVariants(series, auth)
        LOG.info("Created and published part draft for series: ${series.id}, product: ${product.id}")
        return product
    }

    @Transactional
    open suspend fun updatePart(auth: Authentication, updateDto: UpdatePartDto, seriesId: UUID) {
        val series = if (auth.isSupplier()) {
            seriesService.findByIdAndSupplierId(seriesId, auth.supplierId())
                ?: throw IllegalArgumentException("Series $seriesId not found for supplier ${auth.supplierId()}")
        } else {
            seriesService.findById(seriesId)
                ?: throw IllegalArgumentException("Series $seriesId not found")
        }

        require(!series.mainProduct) { "Series $seriesId is a main product and cannot be updated as a part" }

        seriesService.update(
            series.copy(
                title = updateDto.title ?: series.title,
                updatedByUser = auth.name,
                updated = LocalDateTime.now(),
            )
        )
        val product = productService.findAllBySeriesUuid(seriesId).first()

        product.let {
            productService.update(
                product.copy(
                    hmsArtNr = if (!auth.isSupplier()) updateDto.hmsArtNr else product.hmsArtNr,
                    supplierRef = updateDto.supplierRef ?: product.supplierRef,
                    articleName = updateDto.title ?: product.articleName,
                    updatedByUser = auth.name,
                    updated = LocalDateTime.now(),
                )
            )
        }
    }

    open suspend fun approvePart(auth: Authentication, seriesId: UUID) {

        val seriesToUpdate = if (auth.isSupplier()) {
            seriesService.findByIdAndSupplierId(seriesId, auth.supplierId())
                ?: throw IllegalArgumentException("Series $seriesId not found for supplier ${auth.supplierId()}")
        } else {
            seriesService.findById(seriesId)
                ?: throw IllegalArgumentException("Series $seriesId not found")
        }

        if (seriesToUpdate.mainProduct) throw BadRequestException("Series $seriesId is a main product and cannot be approved as a part")

        if (seriesToUpdate.adminStatus == AdminStatus.APPROVED) throw BadRequestException("$seriesId is already approved")
        if (seriesToUpdate.status == SeriesStatus.DELETED) throw BadRequestException("SeriesStatus should not be Deleted")

        seriesService.approveSeriesAndVariants(seriesToUpdate, auth)

        LOG.info("set series to approved: $seriesId")
    }
}