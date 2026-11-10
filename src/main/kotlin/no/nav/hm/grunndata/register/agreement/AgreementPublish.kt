package no.nav.hm.grunndata.register.agreement

import no.nav.hm.grunndata.register.logging.sanitizeForLog
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.ServiceAgreementStatus
import no.nav.hm.grunndata.rapid.dto.ServiceStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.servicejob.ServiceAgreementRepository
import no.nav.hm.grunndata.register.servicejob.ServiceJobService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
open class AgreementPublish(
    private val agreementRegigstrationService: AgreementRegistrationService,
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
    private val serviceJobService: ServiceJobService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementPublish::class.java)
    }

    suspend fun publishAgreements(): List<AgreementRegistrationDTO> {
        val publishList = agreementRegigstrationService.findAgreementsToBePublish()
        LOG.info("Found {} agreements to be publish", publishList.size)
        publishList.forEach {
            publishAgreement(it)
        }
        return publishList
    }

    @Transactional
    open suspend fun publishAgreement(agreementRegistrationDTO: AgreementRegistrationDTO) {
        LOG.info("Publishing agreement {} {}", agreementRegistrationDTO.id, agreementRegistrationDTO.reference.sanitizeForLog())
        agreementRegigstrationService.saveAndCreateEventIfNotDraft(
            dto =
                agreementRegistrationDTO.copy(
                    agreementStatus = AgreementStatus.ACTIVE,
                    updated = LocalDateTime.now(),
                    updatedBy = REGISTER,
                    updatedByUser = "system-publish",
                ),
            isUpdate = true,
        )
        val productsInAgreement =
            productAgreementRegistrationService
                .findByAgreementIdAndStatusAndPublishedBeforeAndExpiredAfter(
                    agreementRegistrationDTO.id,
                    ProductAgreementStatus.INACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )

        LOG.info("Found {} products in agreement {}", productsInAgreement.size, agreementRegistrationDTO.id)
        productsInAgreement.forEach { product ->
            LOG.info("Found product: {} in agreement", product.id)
            productAgreementRegistrationService.saveAndCreateEvent(
                product.copy(
                    status = ProductAgreementStatus.ACTIVE,
                    updated = LocalDateTime.now(),
                    updatedBy = REGISTER,
                    updatedByUser = "system-publish"
                ),
                isUpdate = true,
            )
        }

        val serviceAgreements = serviceJobService.findByAgreementIdAndStatusAndPublishedBeforeAndExpiredAfter(
            agreementRegistrationDTO.id,
            ServiceAgreementStatus.INACTIVE,
            LocalDateTime.now(),
            LocalDateTime.now()
        )

        LOG.info("Found {} serviceAgreements in agreement {}", serviceAgreements.size, agreementRegistrationDTO.id)
        serviceAgreements.forEach { serviceAgreement ->
            LOG.info("Found serviceAgreement: {} in agreement", serviceAgreement.id)
            serviceJobService.saveServiceAgreement(
                serviceAgreement.copy(
                    status = ServiceAgreementStatus.ACTIVE,
                    updated = LocalDateTime.now(),
                    updatedBy = REGISTER,
                    updatedByUser = "system-publish"
                ),
                isUpdate = true,
            )
        }

        val serviceJobsInAgreement = serviceJobService.findServiceJobEntitiesByAgreementId(agreementRegistrationDTO.id)
        serviceJobsInAgreement.forEach { serviceJob ->
            LOG.info("Found serviceJob: {} in agreement", serviceJob.id)
            serviceJobService.saveAndCreateEventIfNotDraft(
                serviceJob.copy(
                    status = ServiceStatus.ACTIVE,
                    updated = LocalDateTime.now(),
                    updatedBy = REGISTER,
                    updatedByUser = "system-publish"
                ),
                isUpdate = true,
            )
        }
    }
}
