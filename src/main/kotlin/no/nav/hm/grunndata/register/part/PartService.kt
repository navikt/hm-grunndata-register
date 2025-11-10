package no.nav.hm.grunndata.register.part

import io.micronaut.security.authentication.Authentication
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.isSupplier
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

@Singleton
open class PartService(
    private val seriesService: SeriesRegistrationService,
    private val productService: ProductRegistrationService,
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val catalogImportRepository: CatalogImportRepository
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(PartService::class.java)
    }

    sealed class ChangeToMainProductResult {
        object Ok : ChangeToMainProductResult()
        object NotFound : ChangeToMainProductResult()
    }

    open suspend fun changeToMainProduct(seriesUUID: UUID, newIsoCode: String): ChangeToMainProductResult {
        val series = seriesService.findById(seriesUUID) ?: return ChangeToMainProductResult.NotFound

        val updatedSeries = series.copy(mainProduct = true, isoCategory = newIsoCode)
        val products = productService.findAllBySeriesUuid(seriesUUID)
        products.forEach { product ->
            val updatedProduct =
                product.copy(mainProduct = true, accessory = false, sparePart = false, isoCategory = newIsoCode)
            productService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)
        }
        seriesService.saveAndCreateEventIfNotDraftAndApproved(updatedSeries, isUpdate = true)
        return ChangeToMainProductResult.Ok
    }

    @Transactional
    open suspend fun createDraftWith(auth: Authentication, draft: PartDraftWithDTO): ProductRegistration {
        val series = draft.toSeriesRegistration(auth).also { seriesService.save(it) }
        val product = draft.toProductRegistration(series.id, auth).also { productService.save(it) }
        LOG.info("Created part draft for series: ${series.id}, product: ${product.id}")
        return product
    }

    @Transactional
    open suspend fun createDraftWithAndApprove(auth: Authentication, draft: PartDraftWithDTO): ProductRegistration {
        val series = draft.toSeriesRegistration(auth).also { seriesService.save(it) }
        val product = draft.toProductRegistration(series.id, auth).also { productService.save(it) }
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
        if ((updateDto.hmsArtNr != null && updateDto.hmsArtNr != product.hmsArtNr) ||
            (updateDto.supplierRef != null && updateDto.supplierRef != product.supplierRef)) {
            LOG.info("hmsnr: ${product.hmsArtNr} supplierRef: ${product.supplierRef} was updated $updateDto")
            productAgreementRegistrationRepository.findByProductId(product.id).forEach {
                productAgreementRegistrationRepository.update(it.copy(hmsArtNr = updateDto.hmsArtNr, supplierRef = updateDto.supplierRef ?: it.supplierRef))
            }
            catalogImportRepository.findBySupplierIdAndSupplierRef(product.supplierId, product.supplierRef).forEach {
                catalogImportRepository.update(it.copy(hmsArtNr = updateDto.hmsArtNr ?: it.hmsArtNr, supplierRef = updateDto.supplierRef ?: it.supplierRef))
            }
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