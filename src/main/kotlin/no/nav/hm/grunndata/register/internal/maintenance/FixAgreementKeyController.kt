package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationRepository
import no.nav.hm.grunndata.register.agreement.generateKey
import kotlin.random.Random


@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/agreements")
@Hidden
class FixAgreementKeyController(private val agreementRegistrationRepository: AgreementRegistrationRepository) {


    @Put("/keys")
    suspend fun fixAgreementKeys() {
        val agreements = agreementRegistrationRepository.findAll().collect { agreementRegistration ->
            LOG.info("updating agreement ${agreementRegistration.id} with agreementKey ${agreementRegistration.agreementKey}")
            if (agreementRegistration.agreementKey == null) {
                val agreementKey = generateKey(agreementRegistration.reference)
                agreementRegistrationRepository.update(agreementRegistration.copy(agreementKey = agreementKey))
            }
        }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(FixAgreementKeyController::class.java)
    }

}