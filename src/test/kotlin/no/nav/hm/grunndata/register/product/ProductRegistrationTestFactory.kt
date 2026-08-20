package no.nav.hm.grunndata.register.product

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import java.util.UUID

@Singleton
class ProductRegistrationTestFactory(
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository
) {

    suspend fun createTestProduct(productId: UUID = UUID.randomUUID(),supplierId: UUID = UUID.randomUUID(), seriesUUID: UUID = UUID.randomUUID(), supplierRef : String = UUID.randomUUID().toString(), hmsArtNr: String? = UUID.randomUUID().toString()):  ProductRegistration {
        val series = seriesRegistrationRepository.findById(seriesUUID) ?:
            seriesRegistrationRepository.save(
            SeriesRegistration(
                id = seriesUUID,
                supplierId = supplierId,
                title = "Dette er en serie",
                text = "Dette er en lang beskrivelse av serien",
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                message = "Melding til leverandør",
                updatedByUser = "user",
                createdByUser = "user",
                isoCategory = "12000123",
                seriesData = SeriesDataDTO()
            )
        )
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
        return productRegistrationRepository.save(ProductRegistration(
            id = productId,
            seriesUUID = series.id,
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