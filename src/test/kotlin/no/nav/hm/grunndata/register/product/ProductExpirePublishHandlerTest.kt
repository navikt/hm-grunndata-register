package no.nav.hm.grunndata.register.product

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductExpirePublishHandlerTest(
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val productExpirePublishHandler: ProductExpirePublishHandler) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)
    @Test
    fun handleExpirePublishProducts() {
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
        val seriesUUID = UUID.randomUUID()
        val registration1 =
            ProductRegistration(
                id = UUID.randomUUID(),
                seriesId = seriesUUID.toString(),
                seriesUUID = seriesUUID,
                isoCategory = "12001314",
                supplierId = supplierId,
                registrationStatus = RegistrationStatus.ACTIVE,
                title = "Dette er produkt title",
                articleName = "Dette er produkt 1 med og med",
                hmsArtNr = "1234",
                supplierRef = "eksternref-1234",
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.APPROVED,
                message = "Melding til leverandør",
                adminInfo = null,
                productData = productData,
                updatedByUser = "user",
                createdByUser = "user",
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(1),
                version = 1,
            )
        val registration2 =
            ProductRegistration(
                id = UUID.randomUUID(),
                seriesId = seriesUUID.toString(),
                seriesUUID = seriesUUID,
                isoCategory = "12001314",
                supplierId = supplierId,
                title = "Dette er produkt title 2",
                articleName = "Dette er produkt 2",
                hmsArtNr = "1235",
                supplierRef = "eksternref-1235",
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.APPROVED,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                productData = productData,
                updatedByUser = "user",
                createdByUser = "user",
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().minusYears(1),
                version = 1,
            )
        val registration3 =
            ProductRegistration(
                id = UUID.randomUUID(),
                seriesId = seriesUUID.toString(),
                seriesUUID = seriesUUID,
                isoCategory = "12001314",
                supplierId = supplierId,
                title = "Dette er produkt title 3",
                articleName = "Dette er produkt 3",
                hmsArtNr = "1236",
                supplierRef = "eksternref-1236",
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.APPROVED,
                registrationStatus = RegistrationStatus.INACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                productData = productData,
                updatedByUser = "user",
                createdByUser = "user",
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(2),
                version = 1,
            )


        runBlocking {
            val saved1 = productRegistrationRepository.save(registration1)
            saved1.shouldNotBeNull()
            val saved2 = productRegistrationRepository.save(registration2)
            saved2.shouldNotBeNull()
            val saved3 = productRegistrationRepository.save(registration3)
            saved3.shouldNotBeNull()
            val expired = productExpirePublishHandler.expiredProducts()
            expired.find { it.id == registration2.id }.shouldNotBeNull()
            val published = productExpirePublishHandler.publishProducts()
            published.find { it.id == registration3.id }.shouldNotBeNull()
        }
    }
}