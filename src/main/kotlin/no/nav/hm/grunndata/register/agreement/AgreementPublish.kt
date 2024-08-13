package no.nav.hm.grunndata.register.agreement

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import org.slf4j.LoggerFactory

@Singleton
open class AgreementPublish(private val agreementRegigstrationService: AgreementRegistrationService,
                       private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementPublish::class.java)
    }


    suspend fun publishAgreements(): List<AgreementRegistrationDTO> {
        val publishList = agreementRegigstrationService.findAgreementsToBePublish()
        LOG.info("Found ${publishList.size} agreements to be publish")
        publishList.forEach {
            publishAgreement(it)
        }
        return publishList
    }

    @Transactional
    open suspend fun publishAgreement(agreementRegistrationDTO: AgreementRegistrationDTO) {
        LOG.info("Publishing agreement ${agreementRegistrationDTO.id} ${agreementRegistrationDTO.reference}")
        agreementRegigstrationService.saveAndCreateEventIfNotDraft(dto = agreementRegistrationDTO.copy(agreementStatus = AgreementStatus.ACTIVE,
            updated = LocalDateTime.now(), updatedBy = REGISTER, updatedByUser = "system-publish"), isUpdate = true)
        val productsInAgreement = productAgreementRegistrationService
            .findByAgreementIdAndStatus(agreementRegistrationDTO.id,
                ProductAgreementStatus.INACTIVE)
        productsInAgreement.forEach { product ->
            LOG.info("Found product: ${product.id} in agreement")
            productAgreementRegistrationService.saveAndCreateEvent(product.copy(status = ProductAgreementStatus.ACTIVE,
                updated = LocalDateTime.now(), updatedBy = REGISTER, updatedByUser = "system-publish",
                published = agreementRegistrationDTO.published, expired = agreementRegistrationDTO.expired
            ), isUpdate = true)
        }
    }
}