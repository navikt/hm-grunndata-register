package no.nav.hm.grunndata.register.product

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.iso.IsoVersion
import no.nav.hm.grunndata.register.product.batch.ProductExcelExport
import no.nav.hm.grunndata.register.product.batch.ProductExcelExportDto
import no.nav.hm.grunndata.register.product.batch.ProductExcelImport
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.grunndata.register.techlabel.LabelService
import no.nav.hm.grunndata.register.techlabel.TechLabelRegistration
import no.nav.hm.grunndata.register.techlabel.TechLabelType
import no.nav.hm.grunndata.register.techlabel.toTechLabelDTO
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.UUID

class ProductIsoVersionTest {
    @Test
    fun `product and part DTOs select the requested shared label template`(): Unit = runBlocking {
        val series = SeriesRegistration(
            id = UUID.randomUUID(), supplierId = UUID.randomUUID(),
            title = "Series", text = "", isoCategory = "04360901", isoCategory22 = "04360902",
            seriesData = SeriesDataDTO(),
        )
        val product = ProductRegistration(
            id = UUID.randomUUID(), supplierId = series.supplierId, seriesUUID = series.id,
            articleName = "Variant", supplierRef = "variant", hmsArtNr = null,
            productData = ProductData(techData = listOf(TechData(key = "Shared", value = "42", unit = "cm"))),
        )
        val labels = mockk<LabelService>()
        fun label(name: String) = TechLabelRegistration(
            label = name, isoCode = series.isoCategory, isoCode22 = series.isoCategory22,
            type = TechLabelType.N, unit = "cm", sort = 0, definition = null,
        ).toTechLabelDTO()
        every { labels.fetchLabelsByIsoCode(series.isoCategory) } returns listOf(label("Old"))
        every { labels.fetchLabelsByIsoCode22(series.isoCategory22!!) } returns listOf(label("Shared"), label("New"))
        val seriesService = mockk<SeriesRegistrationService>()
        coEvery { seriesService.findById(series.id) } returns series
        val mapper = ProductDTOMapper(
            mockk(relaxed = true), labels, mockk(), mockk(relaxed = true), seriesService,
        )
        mapper.toDTOV2(product).productData.techData.map { it.key } shouldBe listOf("Old")
        val active = mapper.toDTOV2(product, IsoVersion.v22).productData.techData
        active.map { it.key } shouldBe listOf("New", "Shared")
        active.find { it.key == "Shared" }!!.value shouldBe "42"
        active.find { it.key == "New" }!!.value shouldBe ""
        mapper.toPartDTO(product, IsoVersion.v22).productData.techData shouldBe active

        val exporter = ProductExcelExport(labels)
        val out = ByteArrayOutputStream()
        exporter.createWorkbookToOutputStream(
            listOf(ProductExcelExportDto(
                products = listOf(mapper.toDTO(product)), isoCategory = series.isoCategory22!!,
                seriesUuid = series.id.toString(), seriesTitle = series.title, seriesDescription = series.text,
            )),
            out,
            IsoVersion.v22,
        )
        val imported = ProductExcelImport(labels).importExcelFileForRegistration(
            out.toByteArray().inputStream(), IsoVersion.v22,
        ).single()
        imported.isoCategory22 shouldBe series.isoCategory22
        imported.techData.map { it.key }.toSet() shouldBe setOf("Shared", "New")
        imported.techData.find { it.key == "Shared" }!!.value shouldBe "42"
    }
}
