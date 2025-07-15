package no.nav.hm.grunndata.register.agreement

import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test

@MicronautTest
class AgreementExpirationTest(
    private val agreementExpiration: AgreementExpiration,
    private val agreementService: AgreementRegistrationService,
    private val supplierService: SupplierRegistrationService,
    private val productAgreementService: ProductAgreementRegistrationService,
    private val delkontraktRegistrationService: DelkontraktRegistrationService,
) {
    @MockBean(RapidPushService::class)
    fun mockRapidService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun testAgreementExpiration() {
        runBlocking {
            val agreementId = UUID.randomUUID()
            val agreementId2 = UUID.randomUUID()
            val agreement =
                AgreementRegistrationDTO(
                    draftStatus = DraftStatus.DONE,
                    agreementStatus = AgreementStatus.ACTIVE,
                    id = agreementId,
                    title = "Rammeavtale Rullestoler",
                    reference = "23-10234",
                    published = LocalDateTime.now(),
                    expired = LocalDateTime.now().plusYears(3),
                    agreementData =
                        AgreementData(
                            identifier = "HMDB-123",
                            resume = "En kort beskrivelse",
                            text = "En lang beskrivelse",
                        ),
                    createdByUser = "Tester-123",
                    updatedByUser = "Tester-123",
                )
            val expired =
                AgreementRegistrationDTO(
                    draftStatus = DraftStatus.DONE,
                    agreementStatus = AgreementStatus.ACTIVE,
                    id = agreementId2,
                    title = "Rammeavtale Rullestoler 2",
                    reference = "24-10234",
                    published = LocalDateTime.now(),
                    expired = LocalDateTime.now().minusDays(1),
                    agreementData =
                        AgreementData(
                            identifier = "HMDB-124",
                            resume = "En kort beskrivelse",
                            text = "En lang beskrivelse",
                        ),
                    createdByUser = "Tester-123",
                    updatedByUser = "Tester-123",
                )

            val supplier =
                supplierService.save(
                    SupplierRegistrationDTO(
                        name = "supplier 1",
                        identifier = "unik-identifier",
                        status = SupplierStatus.ACTIVE,
                        supplierData = SupplierData(email = "test@test"),
                    ),
                )


            val delkontraktToSave = DelkontraktRegistrationDTO(
                agreementId = agreement.id,
                delkontraktData = DelkontraktData(title = "delkontrakt 1", description = "beskrivelse", sortNr = 1),
                createdBy = "tester",
                updatedBy = "tester",
                id = UUID.randomUUID(),
                identifier = UUID.randomUUID().toString(),
            )

            val delkontrakt = delkontraktRegistrationService.save(delkontraktToSave)

            val productAgreement =
                ProductAgreementRegistrationDTO(
                    productId = null,
                    agreementId = agreement.id,
                    seriesUuid = UUID.randomUUID(),
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
                    postId = delkontrakt.id,
                    supplierRef = "12345",
                    updatedBy = REGISTER,
                    status = ProductAgreementStatus.ACTIVE
                )

            val productAgreement2 =
                ProductAgreementRegistrationDTO(
                    agreementId = expired.id,
                    productId = null,
                    seriesUuid = UUID.randomUUID(),
                    reference = expired.reference,
                    published = expired.published,
                    expired = expired.expired,
                    post = 1,
                    rank = 2,
                    postId = delkontrakt.id,
                    title = expired.title,
                    articleName = expired.title,
                    createdBy = "tester",
                    hmsArtNr = "123456",
                    supplierId = supplier.id,
                    supplierRef = "123456",
                    updatedBy = REGISTER,
                    status = ProductAgreementStatus.ACTIVE
                )

            productAgreementService.saveAndCreateEvent(productAgreement, false)
            productAgreementService.saveAndCreateEvent(productAgreement2, false)

            agreementService.save(agreement)
            agreementService.save(expired)
            val expiredList = agreementExpiration.expiredAgreements()
            expiredList.size shouldBe 1

            productAgreementService.findByAgreementId(agreement.id).forEach {
                it.expired shouldBeAfter LocalDateTime.now()
                it.status shouldBe ProductAgreementStatus.ACTIVE
            }

            productAgreementService.findByAgreementId(expired.id).forEach {
                it.expired shouldBeBefore LocalDateTime.now()
                it.status shouldBe ProductAgreementStatus.INACTIVE
            }

        }
    }
}
