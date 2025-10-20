package no.nav.hm.grunndata.register.productagreement

import io.kotest.matchers.nulls.shouldNotBeNull
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationService
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.product.ProductRegistrationTestFactory
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test

@MicronautTest
class ProductAgreementExpiredTest(private val productAgreementRegistrationService: ProductAgreementRegistrationService,
                                  private val productRegistrationRepository: ProductRegistrationRepository,
                                  private val productRegistrationTestFactory: ProductRegistrationTestFactory,
                                  private val delkontraktRegistrationService: DelkontraktRegistrationService
) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun testProductAgreementExpired()  {
        runBlocking {
            val productData =
                ProductData(
                    attributes =
                        Attributes(
                            shortdescription = "En kort beskrivelse av produktet",
                            text = "En lang beskrivelse av produktet",
                        ),
                    techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
                    media =
                        setOf(
                            MediaInfoDTO(
                                uri = "123.jpg",
                                text = "bilde av produktet",
                                source = MediaSourceType.EXTERNALURL,
                                sourceUri = "https://ekstern.url/123.jpg",
                            ),
                            MediaInfoDTO(
                                uri = "124.jpg",
                                text = "bilde av produktet 2",
                                source = MediaSourceType.EXTERNALURL,
                                sourceUri = "https://ekstern.url/124.jpg",
                            ),
                        ),
                )
            val supplierId = UUID.randomUUID()
            val supplierRef = "eksternref-14324"
            val seriesUUID = UUID.randomUUID()
            val product = productRegistrationTestFactory.createTestProduct(supplierId, seriesUUID, supplierRef, null)
            val hmsNr = "12345678"
            val registration =
                ProductRegistration(
                    id = product.id,
                    seriesUUID = seriesUUID,
                    isoCategory = "12001314",
                    supplierId = supplierId,
                    title = "Dette er produkt title",
                    articleName = "Dette er produkt 1 med og med",
                    hmsArtNr = null,
                    supplierRef = supplierRef,
                    draftStatus = DraftStatus.DRAFT,
                    adminStatus = AdminStatus.PENDING,
                    registrationStatus = RegistrationStatus.ACTIVE,
                    message = "Melding til leverandør",
                    adminInfo = null,
                    productData = productData,
                    updatedByUser = "user",
                    createdByUser = "user",
                    version = 1,
                )
            val agreementId = UUID.randomUUID()

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

            val agreement =
                ProductAgreementRegistrationDTO(
                    agreementId = agreementId,
                    hmsArtNr = hmsNr,
                    post = 1,
                    rank = 1,
                    postId = postId,
                    reference = "20-1423",
                    seriesUuid = registration.seriesUUID,
                    supplierId = supplierId,
                    supplierRef = supplierRef,
                    createdBy = "user",
                    title = "Test product agreement",
                    articleName = "Test article",
                    expired = LocalDateTime.now().minusDays(1),
                    productId =product.id,
                    published = LocalDateTime.now(),
                    status = ProductAgreementStatus.ACTIVE,
                    updatedBy = REGISTER
                )
                val savedDelkontrakt = delkontraktRegistrationService.save(delkontraktToSave)
                val savedProductAgreementRegistration = productAgreementRegistrationService.save(agreement)
                productAgreementRegistrationService.deactivateExpiredProductAgreements()
                productAgreementRegistrationService.findByAgreementIdAndStatus(agreementId, ProductAgreementStatus.INACTIVE).shouldNotBeNull()


        }
    }
}