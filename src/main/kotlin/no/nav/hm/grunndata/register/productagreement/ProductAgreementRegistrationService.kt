package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.map


@Singleton
open class ProductAgreementRegistrationService(private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository) {

    @Transactional
    open suspend fun saveAll(dtos: List<ProductAgreementRegistrationDTO>): List<ProductAgreementRegistrationDTO> =
        dtos.map { productAgreementRegistrationRepository.save(it.toEntity())}.map { it.toDTO() }


}