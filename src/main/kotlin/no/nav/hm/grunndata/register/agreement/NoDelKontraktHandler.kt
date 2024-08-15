package no.nav.hm.grunndata.register.agreement

import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class NoDelKontraktHandler(private val agreementRegistrationService: AgreementRegistrationService,
                           private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository) {


    suspend fun findAndCreateWithNoDelkonktraktTypeIfNotExists(agreementId: UUID): DelkontraktRegistration {
        return delkontraktRegistrationRepository.findByAgreementIdAndType(agreementId, DelkontraktType.WITH_NO_DELKONTRAKT)
            ?: delkontraktRegistrationRepository.save(
                DelkontraktRegistration(
                    agreementId = agreementId,
                    type = DelkontraktType.WITH_NO_DELKONTRAKT,
                    delkontraktData = DelkontraktData(title = "Ingen delkontrakt",
                        description = "Produkter som ikke er under en delkontrakt",
                        sortNr = 99, refNr = "99")
                )
            )
    }
}