package no.nav.hm.grunndata.register.agreement

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class AgreementExpirationTest(
    private val agreementExpiration: AgreementExpiration,
    private val agreementService: AgreementRegistrationService,
    private val supplierService: SupplierRegistrationService,
    private val productAgreementService: ProductAgreementRegistrationService,
) {


    @MockBean(RapidPushService::class)
    fun mockRapidService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun testAgreementExpiration() {
        runBlocking {
            val agreementId = UUID.randomUUID()
            val agreementId2 = UUID.randomUUID()
            val agreement = AgreementRegistrationDTO(
                draftStatus = DraftStatus.DONE, agreementStatus = AgreementStatus.ACTIVE,
                id = agreementId, title = "Rammeavtale Rullestoler", reference = "23-10234",
                published = LocalDateTime.now(), expired = LocalDateTime.now().plusYears(3),
                agreementData = AgreementData(
                    identifier = "HMDB-123",
                    resume = "En kort beskrivelse",
                    text = "En lang beskrivelse"
                ),
                createdByUser = "Tester-123", updatedByUser = "Tester-123"
            )
            val expired = AgreementRegistrationDTO(
                draftStatus = DraftStatus.DONE, agreementStatus = AgreementStatus.ACTIVE,
                id = agreementId2, title = "Rammeavtale Rullestoler 2", reference = "24-10234",
                published = LocalDateTime.now(), expired = LocalDateTime.now().minusDays(1),
                agreementData = AgreementData(
                    identifier = "HMDB-124",
                    resume = "En kort beskrivelse",
                    text = "En lang beskrivelse"
                ),
                createdByUser = "Tester-123", updatedByUser = "Tester-123"
            )

            val supplier = supplierService.save(
                SupplierRegistrationDTO(
                    name = "supplier 1", identifier = "unik-identifier",
                    status = SupplierStatus.ACTIVE, supplierData = SupplierData(email = "test@test")
                )
            )

            val productAgreement = ProductAgreementRegistrationDTO(
                agreementId = agreement.id,
                productId = UUID.randomUUID(),
                seriesId = UUID.randomUUID().toString(),
                reference = agreement.reference,
                published = agreement.published,
                expired = agreement.expired,
                post = 1,
                rank = 1,
                title = agreement.title,
                articleName = agreement.title,
                createdBy = "tester",
                hmsArtNr = "12345",
                supplierId = supplier.id,
                supplierRef = "12345"
            )

            val productAgreement2 = ProductAgreementRegistrationDTO(
                agreementId = expired.id,
                productId = UUID.randomUUID(),
                seriesId = UUID.randomUUID().toString(),
                reference = expired.reference,
                published = expired.published,
                expired = expired.expired,
                post = 1,
                rank = 2,
                title = expired.title,
                articleName = expired.title,
                createdBy = "tester",
                hmsArtNr = "123456",
                supplierId = supplier.id,
                supplierRef = "123456"
            )

            productAgreementService.saveAndCreateEvent(productAgreement, false)
            productAgreementService.saveAndCreateEvent(productAgreement2, false)

            agreementService.save(agreement)
            agreementService.save(expired)
            val expiredList = agreementExpiration.expiredAgreements()
            expiredList.size shouldBe 1

        }

    }
}
