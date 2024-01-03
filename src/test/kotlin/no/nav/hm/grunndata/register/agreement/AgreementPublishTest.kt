package no.nav.hm.grunndata.register.agreement

import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class AgreementPublishTest(private val agreementPublish: AgreementPublish,
                           private val agreementService: AgreementRegistrationService,
                           private val productAgreementService: ProductAgreementRegistrationService
) {


    @MockBean(RapidPushService::class)
    fun mockRapidService(): RapidPushService = mockk(relaxed = true)


    @Test
    fun handleAgreementPublish() {
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
            val publishing = AgreementRegistrationDTO(
                draftStatus = DraftStatus.DONE, agreementStatus = AgreementStatus.INACTIVE,
                id = agreementId2, title = "Rammeavtale Rullestoler 2", reference = "24-10234",
                published = LocalDateTime.now(), expired = LocalDateTime.now().plusYears(1),
                agreementData = AgreementData(
                    identifier = "HMDB-124",
                    resume = "En kort beskrivelse",
                    text = "En lang beskrivelse"
                ),
                createdByUser = "Tester-123", updatedByUser = "Tester-123"
            )
            val supplier = SupplierRegistrationDTO(
                id = UUID.randomUUID(),
                name = "Test Supplier",
                status = SupplierStatus.ACTIVE,
                supplierData = SupplierData(
                    email = "test@test.test"
                ),
                createdByUser = "Tester-123",
                updatedByUser = "Tester-123"
            )
            val productAgreement = ProductAgreementRegistrationDTO(
                agreementId = agreement.id,
                productId = UUID.randomUUID(),
                reference = agreement.reference,
                published = agreement.published,
                expired = agreement.expired,
                post = 1,
                rank = 1,
                title = agreement.title,
                createdBy = "tester",
                hmsArtNr = "12345",
                supplierId = supplier.id,
                supplierRef = "12345"
            )

            val productAgreement2 = ProductAgreementRegistrationDTO(
                agreementId = publishing.id,
                productId = UUID.randomUUID(),
                reference = publishing.reference,
                published = publishing.published,
                expired = publishing.expired,
                post = 1,
                rank = 2,
                title = publishing.title,
                createdBy = "tester",
                hmsArtNr = "123456",
                supplierId = supplier.id,
                supplierRef = "123456"
            )

            agreementService.saveAndCreateEventIfNotDraft(agreement, false)
            agreementService.saveAndCreateEventIfNotDraft(publishing, false)
            productAgreementService.saveAndCreateEvent(productAgreement, false)
            productAgreementService.saveAndCreateEvent(productAgreement2, false)

            val publishList = agreementPublish.publishAgreements()

            publishList.size shouldBe 1

        }
    }
}