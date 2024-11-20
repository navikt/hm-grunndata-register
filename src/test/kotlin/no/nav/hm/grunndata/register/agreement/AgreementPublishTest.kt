package no.nav.hm.grunndata.register.agreement

import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test

@MicronautTest
class AgreementPublishTest(private val agreementPublish: AgreementPublish,
                           private val agreementService: AgreementRegistrationService,
                           private val productAgreementService: ProductAgreementRegistrationService,
                           private val delkontraktRegistrationService: DelkontraktRegistrationService
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

            val postId = UUID.randomUUID()

            val delkontraktToSave =
                DelkontraktRegistrationDTO(
                    id = postId,
                    agreementId = agreementId,
                    delkontraktData = DelkontraktData(title = "delkontrakt 1", description = "beskrivelse", sortNr = 1),
                    createdBy = "tester",
                    updatedBy = "tester",
                    identifier = postId.toString()
                )

            val productAgreement = ProductAgreementRegistrationDTO(
                agreementId = agreement.id,
                productId = null,
                seriesUuid = UUID.randomUUID(),
                reference = agreement.reference,
                published = agreement.published,
                expired = agreement.expired,
                post = 1,
                rank = 1,
                postId = postId,
                title = agreement.title,
                articleName = agreement.title,
                createdBy = "tester",
                hmsArtNr = "12345",
                supplierId = supplier.id,
                supplierRef = "12345",
                updatedBy = REGISTER
            )

            val productAgreement2 = ProductAgreementRegistrationDTO(
                agreementId = publishing.id,
                productId = null,
                seriesUuid = UUID.randomUUID(),
                reference = publishing.reference,
                published = publishing.published,
                expired = publishing.expired,
                post = 1,
                rank = 2,
                postId = postId,
                title = publishing.title,
                articleName = publishing.title,
                createdBy = "tester",
                hmsArtNr = "123456",
                supplierId = supplier.id,
                supplierRef = "123456",
                updatedBy = REGISTER
            )

            val expiredProductAgreement = ProductAgreementRegistrationDTO(
                agreementId = publishing.id,
                productId = null,
                seriesUuid = UUID.randomUUID(),
                reference = publishing.reference,
                published = publishing.published,
                expired = LocalDateTime.now().minusDays(1),
                status = ProductAgreementStatus.INACTIVE,
                post = 1,
                rank = 3,
                postId = postId,
                title = publishing.title,
                articleName = publishing.title,
                createdBy = "tester",
                hmsArtNr = "123456",
                supplierId = supplier.id,
                supplierRef = "1234567",
                updatedBy = REGISTER
            )


            agreementService.saveAndCreateEventIfNotDraft(agreement, false)
            agreementService.saveAndCreateEventIfNotDraft(publishing, false)
            val savedDelkontrakt = delkontraktRegistrationService.save(delkontraktToSave)
            productAgreementService.saveAndCreateEvent(productAgreement, false)
            productAgreementService.saveAndCreateEvent(productAgreement2, false)
            productAgreementService.saveAndCreateEvent(expiredProductAgreement, false)

            val publishList = agreementPublish.publishAgreements()

            publishList.size shouldBe 1

            productAgreementService.findByAgreementId(publishing.id).forEach {
                if (it.supplierRef == "1234567") {
                    it.status shouldBe ProductAgreementStatus.INACTIVE
                    it.published shouldBeBefore LocalDateTime.now()
                    it.expired shouldBeBefore LocalDateTime.now()
                }
                else {
                    it.status shouldBe ProductAgreementStatus.ACTIVE
                    it.published shouldBeBefore LocalDateTime.now()
                    it.expired shouldBeAfter LocalDateTime.now()
                }
            }

        }
    }
}