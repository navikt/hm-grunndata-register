package no.nav.hm.grunndata.register.agreement

import jakarta.inject.Singleton
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.DelkontraktType
import org.slf4j.LoggerFactory

@Singleton
class NoDelKontraktHandler(private val agreementRegistrationService: AgreementRegistrationService,
                           private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository) {

    companion object {
        private val LOG = LoggerFactory.getLogger(NoDelKontraktHandler::class.java)
    }
    suspend fun findAndCreateWithNoDelkonktraktTypeIfNotExists(agreementId: UUID): DelkontraktRegistration {
        return delkontraktRegistrationRepository.findByAgreementIdAndType(agreementId, DelkontraktType.WITH_NO_DELKONTRAKT)
            ?: run {
                LOG.info("Creating delkontrakt with no delkontrakt type for agreementId: $agreementId")
                delkontraktRegistrationRepository.save(
                    DelkontraktRegistration(
                        agreementId = agreementId,
                        type = DelkontraktType.WITH_NO_DELKONTRAKT,
                        delkontraktData = DelkontraktData(title = "99: Ingen delkontrakt",
                            description = "Produkter som ikke er under en delkontrakt",
                            sortNr = 99, refNr = "99")
                    )
                )
            }
    }
}