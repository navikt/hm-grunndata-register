package no.nav.hm.grunndata.register.product

import io.kotest.common.runBlocking
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.event.EventItemService
import no.nav.hm.grunndata.register.event.EventItemType
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductRegistrationHandlerTest(private val productRegistrationHandler: ProductRegistrationHandler,
                                     private val supplierRegistrationService: SupplierRegistrationService,
                                     private val eventItemService: EventItemService) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun testProductRegistrationHandler() {
        runBlocking {
            val testSupplier = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData(
                        address = "address 3",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier3@test.test",
                    ),
                    identifier =  "supplier-123",
                    name =  "name-123",
                )
            )
            val productData = ProductData (
                attributes = Attributes (
                    shortdescription = "En kort beskrivelse av produktet",
                    text = "En lang beskrivelse av produktet"
                ),
                accessory = false,
                sparePart = false,
                techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
                media = setOf(
                    MediaInfo(uri="123.jpg", text = "bilde av produktet", source = MediaSourceType.EXTERNALURL,
                        sourceUri = "https://ekstern.url/123.jpg"),
                    MediaInfo(uri="124.jpg", text = "bilde av produktet 2", source = MediaSourceType.EXTERNALURL,
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
            productRegistrationHandler.queueDTORapidEvent(registration)
            val events = eventItemService.getAllPendingStatus()
            events.size shouldBeGreaterThan 0
            events.forEach {
                if (it.type == EventItemType.PRODUCT) {
                    productRegistrationHandler.sendRapidEvent(it)
                }
            }
        }
    }
}