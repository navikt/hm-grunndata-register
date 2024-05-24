package no.nav.hm.grunndata.register.productagreement

import io.micronaut.context.annotation.Factory
import io.micronaut.data.event.listeners.PostUpdateEventListener
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistration
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Factory
class AgreementPersistListener(private val productAgreementRegistrationService: ProductAgreementRegistrationService) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AgreementPersistListener::class.java)
    }


    @Singleton
    fun afterAgreementUpdate(): PostUpdateEventListener<AgreementRegistration> {
        return PostUpdateEventListener { agreement: AgreementRegistration ->
            runBlocking {
                if (agreement.draftStatus == DraftStatus.DONE) {
                    val pagreements = productAgreementRegistrationService.findByAgreementIdAndStatusAndPublishedBeforeAndExpiredAfter(
                        agreement.id,
                        ProductAgreementStatus.ACTIVE,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                    )
                    LOG.info("Got ${pagreements.size} product agreements from agreement ${agreement.id} for update")
                    pagreements.forEach {
                        if (agreement.published != it.published || agreement.expired != it.expired) {
                            productAgreementRegistrationService.saveAndCreateEvent(
                                it.copy(
                                    status = if (agreement.agreementStatus == AgreementStatus.ACTIVE) ProductAgreementStatus.ACTIVE else ProductAgreementStatus.INACTIVE,
                                    published = agreement.published,
                                    expired = agreement.expired
                                ), isUpdate = true
                            )
                        }
                    }
                }
            }
        }
    }
}