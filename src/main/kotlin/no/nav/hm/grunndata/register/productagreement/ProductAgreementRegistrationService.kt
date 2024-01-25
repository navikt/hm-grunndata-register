package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import java.time.LocalDateTime
import java.util.UUID


@Singleton
open class ProductAgreementRegistrationService(
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val productAgreementRegistrationHandler: ProductAgreementRegistrationEventHandler
) {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ProductAgreementRegistrationService::class.java)
    }

    @Transactional
    open suspend fun saveAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement ->
            findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
                productAgreement.supplierId,
                productAgreement.supplierRef,
                productAgreement.agreementId,
                productAgreement.post,
                productAgreement.rank
            ) ?: saveAndCreateEvent(productAgreement, false)
        }

    @Transactional
    open suspend fun saveOrUpdateAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement ->
            findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
                productAgreement.supplierId,
                productAgreement.supplierRef,
                productAgreement.agreementId,
                productAgreement.post,
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
                        expired = productAgreement.expired
                    )
                )
            } ?: saveAndCreateEvent(productAgreement, false)
        }


    suspend fun save(dto: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO =
        productAgreementRegistrationRepository.save(dto.toEntity()).toDTO()

    suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
        supplierId: UUID, supplierRef: String, agreementId: UUID, post: Int, rank: Int
    ): ProductAgreementRegistrationDTO? =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
            supplierId, supplierRef, agreementId, post, rank
        )?.toDTO()

    suspend fun findBySupplierIdAndSupplierRef(
        supplierId: UUID,
        supplierRef: String
    ): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef)
            .map { it.toDTO() }

    suspend fun findById(id: UUID): ProductAgreementRegistrationDTO? =
        productAgreementRegistrationRepository.findById(id)?.toDTO()

    suspend fun findByAgreementId(agreementId: UUID): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByAgreementId(agreementId).map { it.toDTO() }

    suspend fun findGroupedProductVariantsByAgreementId(agreementId: UUID): List<ProduktvarianterForDelkontrakterDTO> {
        val alleVarianter = productAgreementRegistrationRepository.findByAgreementIdAndStatus(
            agreementId,
            ProductAgreementStatus.ACTIVE
        ).map { it.toDTO() }

        val liste = mutableListOf<ProduktvarianterForDelkontrakterDTO>()


        alleVarianter.groupBy { it.post }.map { (post, produkterIPost) ->
            produkterIPost.groupBy { it.seriesUuid }.map { (_, varianter) ->
                liste.add(
                    ProduktvarianterForDelkontrakterDTO(
                        delkontraktNr = post,
                        produktTittel = varianter.first().title,
                        produktvarianter = varianter,
                        rangering = varianter.first().rank,
                        produktserie = varianter.first().seriesUuid
                    )
                )
            }
        }

        liste.sortBy { it.rangering }
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
        isUpdate: Boolean
    ): ProductAgreementRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (dto.productId != null) {
            productAgreementRegistrationHandler.queueDTORapidEvent(
                saved,
                eventName = EventName.registeredProductAgreementV1
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
                            updated = LocalDateTime.now()
                        )
                    )
                }
        }
    }

}

data class ProduktvarianterForDelkontrakterDTO(
    val delkontraktNr: Int,
    val produktTittel: String,
    val rangering: Int,
    val produktserie: UUID?,
    val produktvarianter: List<ProductAgreementRegistrationDTO>
)