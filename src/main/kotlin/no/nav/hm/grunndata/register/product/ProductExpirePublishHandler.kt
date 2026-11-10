package no.nav.hm.grunndata.register.product

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.REGISTER
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
class ProductExpirePublishHandler(private val productRegistrationService: ProductRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductExpirePublishHandler::class.java)

    }


    suspend fun expiredProducts(): List<ProductRegistration> {
        val expiredProducts = productRegistrationService.findExpired()
        LOG.info("Found {} products to be expired", expiredProducts.size)
        expiredProducts.forEach {
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                it.copy(
                    registrationStatus = RegistrationStatus.INACTIVE, updatedByUser = "system-expired",
                    updatedBy = REGISTER, updated = LocalDateTime.now()
                ), isUpdate = true
            )
        }
        return expiredProducts
    }

    suspend fun publishProducts(): List<ProductRegistration> {
        val publishing = productRegistrationService.findProductsToPublish()
        LOG.info("Found {} products to be published", publishing.size)
        publishing.forEach {
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                it.copy(
                    registrationStatus = RegistrationStatus.ACTIVE, updatedByUser = "system-published",
                    updatedBy = REGISTER, updated = LocalDateTime.now()
                ), isUpdate = true
            )
        }
        return publishing
    }
}