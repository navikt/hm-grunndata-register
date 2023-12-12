package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.map
import java.util.*


@Singleton
open class ProductAgreementRegistrationService(private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository) {

    @Transactional
    open suspend fun saveAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreement -> findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
                    productAgreement.supplierId,
                    productAgreement.supplierRef,
                    productAgreement.agreementId,
                    productAgreement.post,
                    productAgreement.rank
                )?.let {
                    productAgreementRegistrationRepository.update(productAgreement.toEntity()).toDTO()
            } ?: productAgreementRegistrationRepository.save(productAgreement.toEntity()).toDTO()
        }


    open suspend fun save(dto: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO =
        productAgreementRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
        supplierId: UUID, supplierRef: String, agreementId: UUID, post: Int, rank: Int): ProductAgreementRegistrationDTO? =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
            supplierId, supplierRef, agreementId, post, rank)?.toDTO()

    open suspend fun findBySupplierIdAndSupplierRef(supplierId: UUID, supplierRef: String): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findBySupplierIdAndSupplierRef(supplierId, supplierRef).map { it.toDTO() }


    open suspend fun findByAgreementId(agreementId: UUID): List<ProductAgreementRegistrationDTO> =
        productAgreementRegistrationRepository.findByAgreementId(agreementId).map { it.toDTO() }


}