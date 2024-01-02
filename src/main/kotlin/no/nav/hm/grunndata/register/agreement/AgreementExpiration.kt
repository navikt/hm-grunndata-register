package no.nav.hm.grunndata.db.agreement

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
open class AgreementExpiration(private val agreementService: AgreementRegistrationService,
                               private val productAgreementService: ProductAgreementRegistrationService) {

    companion object {
        private const val expiration = "AGREEMENTEXPIRATION"
        private val LOG = LoggerFactory.getLogger(AgreementExpiration::class.java)

    }
    suspend fun expiredAgreements(): List<AgreementRegistrationDTO> {
        val expiredList = agreementService.findByAgreementStatusAndExpiredBefore(AgreementStatus.ACTIVE)
        LOG.info("Found ${expiredList.size} expired agreements")
        expiredList.forEach {
            deactiveProductsInExpiredAgreement(it)
        }
        return expiredList
    }

    @Transactional
    open suspend fun deactiveProductsInExpiredAgreement(expiredAgreement: AgreementRegistrationDTO) {
        LOG.info("Agreement ${expiredAgreement.id} ${expiredAgreement.reference} has expired")
        agreementService.saveAndCreateEventIfNotDraft(dto = expiredAgreement.copy(agreementStatus = AgreementStatus.INACTIVE,
            updated = LocalDateTime.now(), updatedBy = expiration), isUpdate = true)
        val productsInAgreement = productAgreementService.findByAgreementId(expiredAgreement.id)
        productsInAgreement.forEach { product ->
            LOG.info("Found product: ${product.id} in expired agreement")
            productAgreementService.saveAndCreateEvent(product.copy(status = ProductAgreementStatus.INACTIVE,
                updated = LocalDateTime.now()), isUpdate = true)
        }
    }
}
