package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import java.time.LocalDateTime
import java.util.*

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
        dtos.map { productAgreement ->
            findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
                productAgreement.supplierId,
                productAgreement.supplierRef,
                productAgreement.agreementId,
                productAgreement.postId!!,
                productAgreement.rank,
            ) ?: saveAndCreateEvent(productAgreement, false)
        }

    @Transactional
    open suspend fun saveOrUpdateAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement ->
            findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
                productAgreement.supplierId,
                productAgreement.supplierRef,
                productAgreement.agreementId,
                productAgreement.postId!!,
                productAgreement.rank,
            )?.let { inDb ->
                update(
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
                )
            } ?: saveAndCreateEvent(productAgreement, false)
        }

    @Transactional
    open suspend fun updateAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement ->
            findById(
                productAgreement.id,
            )?.let { inDb ->
                update(
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
                )
            } ?: throw RuntimeException("Product agreement not found")
        }

    suspend fun save(dto: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO =
        productAgreementRegistrationRepository.save(dto.toEntity()).toDTO()

    suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
        supplierId: UUID,
        supplierRef: String,
        agreementId: UUID,
        postId: UUID,
        rank: Int,
    ): ProductAgreementRegistrationDTO? =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
            supplierId,
            supplierRef,
            agreementId,
            postId,
            rank,
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

    suspend fun findByAgreementId(agreementId: UUID): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByAgreementId(agreementId).map { it.toDTO() }

    suspend fun findByDelkontraktId(delkontraktId: UUID): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByPostId(delkontraktId).map { it.toDTO() }

    suspend fun findGroupedProductVariantsByDelkontraktId(delkontraktId: UUID): List<ProductVariantsForDelkontraktDto> {
        val allVariants =
            productAgreementRegistrationRepository.findByPostIdAndStatus(
                delkontraktId,
                ProductAgreementStatus.ACTIVE,
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

    suspend fun deleteById(id: UUID): Int = productAgreementRegistrationRepository.deleteById(id)

    @Transactional
    open suspend fun deleteByIds(ids: List<UUID>): Int {
        ids.forEach { productAgreementRegistrationRepository.deleteById(it) }
        return ids.size
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
        }
        return saved
    }

    suspend fun update(dto: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO {
        return productAgreementRegistrationRepository.update(dto.toEntity()).toDTO()
    }

    open suspend fun connectProductAgreementToProduct() {
        val productAgreementList = productAgreementRegistrationRepository.findByProductIdIsNull()
        LOG.info("Found product agreements with no connection: ${productAgreementList.size}")
        productAgreementList.forEach {
            productRegistrationRepository.findBySupplierRefAndSupplierId(it.supplierRef, it.supplierId)
                ?.let { product ->
                    LOG.info("Found product ${product.id} with supplierRef: ${it.supplierRef} and supplierId: ${it.supplierId}")
                    productAgreementRegistrationRepository.update(
                        it.copy(
                            productId = product.id,
                            updated = LocalDateTime.now(),
                        ),
                    )
                }
        }
    }
}

data class ProductVariantsForDelkontraktDto(
    val postId: UUID,
    val productSeries: UUID?,
    val productTitle: String,
    val serieIdentifier: String?,
    val rank: Int,
    val productVariants: List<ProductAgreementRegistrationDTO>,
)

data class ProduktvarianterForDelkontrakterDTO(
    val delkontraktNr: Int,
    val produktTittel: String,
    val rangering: Int,
    val produktserie: UUID?,
    val serieIdentifier: String?,
    val produktvarianter: List<ProductAgreementRegistrationDTO>,
)
