package no.nav.hm.grunndata.register.agreement

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
class AgreementPublish(private val agreementRegigstrationService: AgreementRegistrationService,
                       private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

    companion object {
        private const val publish = "AGREEMENTPUBLISH"
        private val LOG = LoggerFactory.getLogger(AgreementPublish::class.java)
    }


    suspend fun publishAgreements(): List<AgreementRegistrationDTO> {
        val publishList = agreementRegigstrationService.findByAgreementStatusAndPublishedBeforeAndExpiredAfter(AgreementStatus.INACTIVE)
        LOG.info("Found ${publishList.size} agreements to be publish")
        publishList.forEach {
            publishAgreement(it)
        }
        return publishList
    }

    suspend fun publishAgreement(agreementRegistrationDTO: AgreementRegistrationDTO) {
        LOG.info("Publishing agreement ${agreementRegistrationDTO.id} ${agreementRegistrationDTO.reference}")
        agreementRegigstrationService.saveAndCreateEventIfNotDraft(dto = agreementRegistrationDTO.copy(agreementStatus = AgreementStatus.ACTIVE,
            updated = LocalDateTime.now(), updatedBy = publish), isUpdate = true)
        val productsInAgreement = productAgreementRegistrationService.findByAgreementId(agreementRegistrationDTO.id)
        productsInAgreement.forEach { product ->
            LOG.info("Found product: ${product.id} in agreement")
            productAgreementRegistrationService.saveAndCreateEvent(product.copy(status = ProductAgreementStatus.ACTIVE,
                updated = LocalDateTime.now()), isUpdate = true)
        }
    }
}