package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.map
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import java.util.*


@Singleton
open class ProductAgreementRegistrationService(private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
                                               private val productAgreementRegistrationHandler: ProductAgreementRegistrationHandler) {

    @Transactional
    open suspend fun saveAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement -> findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
                    productAgreement.supplierId,
                    productAgreement.supplierRef,
                    productAgreement.agreementId,
                    productAgreement.post,
                    productAgreement.rank
                ) ?:saveAndCreateEvent(productAgreement, false)
            }


    suspend fun save(dto: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO =
        productAgreementRegistrationRepository.save(dto.toEntity()).toDTO()

    suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
        supplierId: UUID, supplierRef: String, agreementId: UUID, post: Int, rank: Int): ProductAgreementRegistrationDTO? =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
            supplierId, supplierRef, agreementId, post, rank)?.toDTO()

    suspend fun findBySupplierIdAndSupplierRef(supplierId: UUID, supplierRef: String): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef).map { it.toDTO() }

    suspend fun findById(id: UUID): ProductAgreementRegistrationDTO? = productAgreementRegistrationRepository.findById(id)?.toDTO()

    suspend fun findByAgreementId(agreementId: UUID): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByAgreementId(agreementId).map { it.toDTO() }

    suspend fun deleteById(id: UUID): Int = productAgreementRegistrationRepository.deleteById(id)

    @Transactional
    open suspend fun deleteByIds(ids: List<UUID>): Int {
        ids.forEach { productAgreementRegistrationRepository.deleteById(it) }
        return ids.size
    }


    @Transactional
    open suspend fun saveAndCreateEvent(dto: ProductAgreementRegistrationDTO, isUpdate:Boolean): ProductAgreementRegistrationDTO {
        val saved = if (isUpdate) update(dto) else save(dto)
        if (dto.productId!= null) {
            productAgreementRegistrationHandler.queueDTORapidEvent(saved)
        }
        return saved
    }

    suspend fun update(dto: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO {
        return productAgreementRegistrationRepository.update(dto.toEntity()).toDTO()
    }


}