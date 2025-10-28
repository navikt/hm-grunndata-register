package no.nav.hm.grunndata.register.product

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.ProductRapidDTO
import no.nav.hm.grunndata.rapid.dto.ProductRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistration
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test

@MicronautTest
class ProductRegistrationEventHandlerTest(private val productRegistrationEventHandler: ProductRegistrationEventHandler,
                                          private val supplierRepository: SupplierRepository,
                                          private val eventItemService: EventItemService) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun testProductRegistrationHandler() {
        runBlocking {
            val testSupplier = supplierRepository.save(
                SupplierRegistration(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData(
                        address = "address 3",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier3@test.test",
                    ),
                    identifier = "supplier-123",
                    name = "name-123",
                )
            )
            val productData = ProductData (
                attributes = Attributes (
                    shortdescription = "En kort beskrivelse av produktet",
                    text = "En lang beskrivelse av produktet"
                ),
                techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
                media = setOf(
                    MediaInfoDTO(uri="123.jpg", text = "bilde av produktet", source = MediaSourceType.EXTERNALURL,
                        sourceUri = "https://ekstern.url/123.jpg"),
                    MediaInfoDTO(uri="124.jpg", text = "bilde av produktet 2", source = MediaSourceType.EXTERNALURL,
                        sourceUri = "https://ekstern.url/124.jpg")
                )
            )
            val registration = ProductRegistrationDTO(
                seriesId = "series-123",
                seriesUUID = UUID.randomUUID(),
                title = "Dette er produkt 1",
                articleName = "Dette er produkt 1 med og med",
                id = UUID.randomUUID(),
                isoCategory = "12001314",
                supplierId = testSupplier!!.id,
                hmsArtNr = "111",
                supplierRef = "eksternref-111",
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.APPROVED,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                createdByAdmin = false,
                expired = LocalDateTime.now().plusYears(15),
                published = LocalDateTime.now(),
                updatedByUser = "email",
                createdByUser = "email",
                productData = productData,
                version = 1,
                createdBy = REGISTER,
                updatedBy = REGISTER
            )
            productRegistrationEventHandler.queueDTORapidEvent(registration, eventName = EventName.registeredProductV1)
            val events = eventItemService.findByStatusOrderByUpdatedAsc()
            events.size shouldBeGreaterThan 0
            events.forEach {
                if (it.type == EventItemType.PRODUCT) {
                    val sent = productRegistrationEventHandler.sendRapidEvent(it) as ProductRegistrationRapidDTO
                    sent.productDTO.supplier.id shouldBe testSupplier!!.id
                    sent.productDTO.supplier.name shouldBe testSupplier!!.name
                }
            }
        }
    }
}