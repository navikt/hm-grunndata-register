package no.nav.hm.grunndata.register.product

import jakarta.inject.Singleton
import java.util.*
import javax.transaction.Transactional

@Singleton
class ProductRegistrationService(private val productRegistrationRepository: ProductRegistrationRepository,
                                 private val productRegistrationHandler: ProductRegistrationHandler) {


    open suspend fun findById(id: UUID) = productRegistrationRepository.findById(id)?.toDTO()
    
    open suspend fun save(dto: ProductRegistrationDTO) = productRegistrationRepository.save(dto.toEntity()).toDTO()

    open suspend fun update(dto: ProductRegistrationDTO) = productRegistrationRepository.update(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndPushToKafka(dto: ProductRegistrationDTO, isUpdate: Boolean): ProductRegistrationDTO {

        val saved = if (isUpdate) update(dto) else save(dto)
        productRegistrationHandler.pushToRapidIfNotDraft(saved)
        return saved
    }


}