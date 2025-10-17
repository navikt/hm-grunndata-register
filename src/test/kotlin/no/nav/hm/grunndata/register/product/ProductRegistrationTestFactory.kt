package no.nav.hm.grunndata.register.product

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import java.util.UUID

@Singleton
class ProductRegistrationTestFactory(private val productRegistrationRepository: ProductRegistrationRepository) {

    suspend fun createTestProduct(supplierId: UUID = UUID.randomUUID(), seriesUUID: UUID = UUID.randomUUID(), supplierRef : String = UUID.randomUUID().toString(), hmsArtNr: String = UUID.randomUUID().toString()):  ProductRegistration {
        val productData1 =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = "En kort beskrivelse av produktet",
                        text = "En lang beskrivelse av produktet",
                    ),
                techData = listOf(
                    TechData(key = "maksvekt", unit = "kg", value = "120"),
                    TechData(key = "bredde", unit = "cm", value = "120"),
                    TechData(key = "Brukerhøyde maks", unit = "kg", value = "120")
                )
            )
        val seriesUUID = UUID.randomUUID()
        return productRegistrationRepository.save(ProductRegistration(
            id = UUID.randomUUID(),
            seriesUUID = seriesUUID,
            isoCategory = "12001314",
            supplierId = supplierId,
            title = "Dette er produkt title",
            articleName = "Dette er produkt 1 med og med",
            hmsArtNr = hmsArtNr,
            supplierRef = supplierRef,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            productData = productData1,
            updatedByUser = "user",
            createdByUser = "user",
            version = 1,
            accessory = false,
            sparePart = false,
            mainProduct = true
        ))
    }
}