package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository

@Singleton
open class ProductAgreementRegistrationService(
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val productAgreementRegistrationHandler: ProductAgreementRegistrationEventHandler,
) {
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ProductAgreementRegistrationService::class.java)
    }

    @Transactional
    open suspend fun saveAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement -> saveIfNotExists(productAgreement) }

    open suspend fun saveIfNotExists(productAgreement: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO {
        LOG.info("Saving product agreement: ${productAgreement.agreementId} for supplier: ${productAgreement.supplierId} " +
                "and for product: ${productAgreement.productId} supplierRef: ${productAgreement.supplierRef}")
        return findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
                productAgreement.supplierId,
                productAgreement.supplierRef,
                productAgreement.agreementId,
                productAgreement.postId
            ) ?: saveAndCreateEvent(productAgreement, false)
    }

    @Transactional
    open suspend fun saveOrUpdateAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement ->
            findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
                productAgreement.supplierId,
                productAgreement.supplierRef,
                productAgreement.agreementId,
                productAgreement.postId
            )?.let { inDb ->
                saveAndCreateEvent(
                    inDb.copy(
                        productId = productAgreement.productId,
                        seriesUuid = productAgreement.seriesUuid,
                        title = productAgreement.title,
                        articleName = productAgreement.articleName,
                        hmsArtNr = productAgreement.hmsArtNr,
                        reference = productAgreement.reference,
                        status = productAgreement.status,
                        updated = LocalDateTime.now(),
                        published = productAgreement.published,
                        expired = productAgreement.expired,
                        rank = productAgreement.rank,
                    ),
                    true,
                )
            } ?: saveAndCreateEvent(productAgreement, false)
        }

    @Transactional
    open suspend fun updateAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement ->
            findById(
                productAgreement.id,
            )?.let { inDb ->
                saveAndCreateEvent(
                    inDb.copy(
                        agreementId = productAgreement.agreementId,
                        productId = productAgreement.productId,
                        seriesUuid = productAgreement.seriesUuid,
                        title = productAgreement.title,
                        articleName = productAgreement.articleName,
                        hmsArtNr = productAgreement.hmsArtNr,
                        reference = productAgreement.reference,
                        status = productAgreement.status,
                        updated = LocalDateTime.now(),
                        published = productAgreement.published,
                        expired = productAgreement.expired,
                        rank = productAgreement.rank,
                        updatedBy = REGISTER,
                    ),
                    true,
                )
            } ?: throw RuntimeException("Product agreement not found")
        }

    suspend fun save(dto: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO =
        productAgreementRegistrationRepository.save(dto.toEntity()).toDTO()


    suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
        supplierId: UUID,
        supplierRef: String,
        agreementId: UUID,
        postId: UUID?
    ): ProductAgreementRegistrationDTO? =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
            supplierId,
            supplierRef,
            agreementId,
            postId,
        )?.toDTO()

    suspend fun findBySupplierIdAndSupplierRef(
        supplierId: UUID,
        supplierRef: String,
    ): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)
            .map { it.toDTO() }

    suspend fun findById(id: UUID): ProductAgreementRegistrationDTO? = productAgreementRegistrationRepository.findById(id)?.toDTO()

    suspend fun findAllByIds(ids: List<UUID>): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findAllByIdIn(ids).toDTO()

    suspend fun findAllByProductIds(ids: List<UUID>): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findAllByProductIdIn(ids).toDTO()

    suspend fun findByAgreementId(agreementId: UUID): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByAgreementId(agreementId).map { it.toDTO() }

    suspend fun findByAgreementIdAndStatusAndPublishedBeforeAndExpiredAfter(
        agreementId: UUID,
        status: ProductAgreementStatus,
        published: LocalDateTime,
        expired: LocalDateTime,
    ): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByAgreementIdAndStatusAndPublishedBeforeAndExpiredAfter(
            agreementId,
            status,
            published,
            expired,
        ).map { it.toDTO() }

    suspend fun findByAgreementIdAndStatusAndExpiredBefore(
        agreementId: UUID,
        status: ProductAgreementStatus,
        expired: LocalDateTime,
    ): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByAgreementIdAndStatusAndExpiredBefore(
            agreementId,
            status,
            expired,
        ).map { it.toDTO() }

    suspend fun findGroupedProductVariantsByDelkontraktId(delkontraktId: UUID): List<ProductVariantsForDelkontraktDto> {
        val allVariants =
            productAgreementRegistrationRepository.findByPostIdAndStatusIn(
                delkontraktId,
                listOf(ProductAgreementStatus.ACTIVE, ProductAgreementStatus.INACTIVE),
            ).map { it.toDTO() }

        val groupedList = mutableListOf<ProductVariantsForDelkontraktDto>()

        allVariants.filter { it.seriesUuid != null }.groupBy { it.seriesUuid }.map { (seriesUuid, varianter) ->
            val seriesInfo = seriesUuid?.let { seriesRegistrationRepository.findById(it) }

            groupedList.add(
                ProductVariantsForDelkontraktDto(
                    postId = delkontraktId,
                    productSeries = seriesUuid,
                    productTitle = seriesInfo?.title ?: "",
                    serieIdentifier = seriesInfo?.identifier,
                    rank = varianter.first().rank,
                    productVariants = varianter,
                    accessory = varianter.first().accessory,
                    sparePart = varianter.first().sparePart,
                ),
            )
        }

        allVariants.filter { it.seriesUuid == null }.map { variant ->
            groupedList.add(
                ProductVariantsForDelkontraktDto(
                    postId = delkontraktId,
                    productSeries = null,
                    productTitle = variant.title,
                    serieIdentifier = null,
                    rank = variant.rank,
                    productVariants = listOf(variant),
                    accessory = variant.accessory,
                    sparePart = variant.sparePart,
                ),
            )
        }

        return groupedList.sortedBy { it.rank }
    }

    suspend fun findGroupedProductVariantsByAgreementId(agreementId: UUID): List<ProduktvarianterForDelkontrakterDTO> {
        val alleVarianter =
            productAgreementRegistrationRepository.findByAgreementIdAndStatus(
                agreementId,
                ProductAgreementStatus.ACTIVE,
            ).map { it.toDTO() }

        val liste = mutableListOf<ProduktvarianterForDelkontrakterDTO>()

        alleVarianter.groupBy { it.post }.map { (post, produkterIPost) ->
            produkterIPost.groupBy { it.seriesUuid }.map { (seriesUuid, varianter) ->
                val seriesInfo = seriesUuid?.let { seriesRegistrationRepository.findById(it) }
                liste.add(
                    ProduktvarianterForDelkontrakterDTO(
                        delkontraktNr = post,
                        produktTittel = seriesInfo?.title ?: "",
                        produktvarianter = varianter,
                        rangering = varianter.first().rank,
                        produktserie = seriesUuid,
                        serieIdentifier = seriesInfo?.identifier,
                    ),
                )
            }
        }

        liste.sortBy { it.delkontraktNr }
        return liste
    }

    @Transactional
    open suspend fun saveAndCreateEvent(
        dto: ProductAgreementRegistrationDTO,
        isUpdate: Boolean,
    ): ProductAgreementRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (dto.productId != null) {
            productAgreementRegistrationHandler.queueDTORapidEvent(
                saved,
                eventName = EventName.registeredProductAgreementV1,
            )
        } else {
            LOG.info("This product does not have a product id, will not create event")
        }
        return saved
    }

    suspend fun update(dto: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO {
        return productAgreementRegistrationRepository.update(dto.toEntity()).toDTO()
    }

    suspend fun physicalDeleteById(id: UUID): Int {
        return productAgreementRegistrationRepository.deleteById(id)
    }


    suspend fun findByAgreementIdAndStatus(
        id: UUID,
        status: ProductAgreementStatus,
    ): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByAgreementIdAndStatus(id, status).map { it.toDTO() }

    suspend fun findByStatusAndExpiredBefore(status: ProductAgreementStatus, expired: LocalDateTime) =
        productAgreementRegistrationRepository.findByStatusAndExpiredBefore(status, expired).map { it.toDTO() }

    suspend fun findBystatusAndPublishedAfter(status: ProductAgreementStatus, published: LocalDateTime) =
        productAgreementRegistrationRepository.findByStatusAndPublishedAfter(status, published).map { it.toDTO() }

    suspend fun findByStatusAndPublishedBeforeAndExpiredAfter(status: ProductAgreementStatus, published: LocalDateTime, expired: LocalDateTime) =
        productAgreementRegistrationRepository.findByStatusAndPublishedBeforeAndExpiredAfter(status, published, expired).map { it.toDTO() }

    suspend fun deactivateExpiredProductAgreements() {
        val products = findByStatusAndExpiredBefore(
            ProductAgreementStatus.ACTIVE, LocalDateTime.now())
        LOG.info("Found ${products.size} products to deactivate")
        products.forEach {
            saveAndCreateEvent(
                it.copy(status = ProductAgreementStatus.INACTIVE,
                    updated = LocalDateTime.now(),
                    updatedByUser = "system-expired"), true
            )
        }
    }

    suspend fun deactivateProductExpiredActiveAgreements() {
        val products = findProductExpiredButActiveAgreements()
        LOG.info("Found ${products.size} products to deactivate")
        products.forEach {
            saveAndCreateEvent(
                it.copy(status = ProductAgreementStatus.INACTIVE,
                    expired = LocalDateTime.now(),
                    updated = LocalDateTime.now(),
                    updatedByUser = "system"), true
            )
        }
    }

    suspend fun findProductExpiredButActiveAgreements(): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findProductExpiredButActiveAgreements().map { it.toDTO() }

}

data class ProductVariantsForDelkontraktDto(
    val postId: UUID,
    val productSeries: UUID?,
    val productTitle: String,
    val serieIdentifier: String?,
    val rank: Int,
    val productVariants: List<ProductAgreementRegistrationDTO>,
    val accessory: Boolean,
    val sparePart: Boolean,
)

data class ProduktvarianterForDelkontrakterDTO(
    val delkontraktNr: Int,
    val produktTittel: String,
    val rangering: Int,
    val produktserie: UUID?,
    val serieIdentifier: String?,
    val produktvarianter: List<ProductAgreementRegistrationDTO>,
)
