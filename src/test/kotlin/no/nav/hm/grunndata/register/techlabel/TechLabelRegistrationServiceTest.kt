package no.nav.hm.grunndata.register.techlabel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.hm.grunndata.rapid.dto.TechData
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.iso.IsoVersion
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.junit.jupiter.api.Test
import java.util.UUID

class TechLabelRegistrationServiceTest {
    private val repository = mockk<TechLabelRegistrationRepository>(relaxed = true)
    private val products = mockk<ProductRegistrationService>()
    private val service = TechLabelRegistrationService(repository, products)
    private val label = TechLabelRegistration(
        label = "Height", isoCode = "09070601", isoCode22 = "09070602",
        type = TechLabelType.N, unit = "cm", sort = 1, definition = null,
    )
    private val product = mockk<ProductRegistration> {
        every { id } returns UUID.randomUUID()
    }

    @Test
    fun `ISO22 delete without mapped code still blocks ISO16 usage`() = runBlocking {
        val unmapped = label.copy(isoCode22 = null)
        coEvery {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode, label.label, label.unit, IsoVersion.v16)
        } returns listOf(product)

        shouldThrow<IllegalStateException> {
            service.delete(unmapped, false, IsoVersion.v22)
        }

        coVerify(exactly = 0) { repository.delete(any<TechLabelRegistration>()) }
    }

    @Test
    fun `default delete blocks ISO22 usage and counts duplicate products once`() = runBlocking {
        coEvery {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode, label.label, label.unit, IsoVersion.v16)
        } returns emptyList()
        coEvery {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode22!!, label.label, label.unit, IsoVersion.v22)
        } returns listOf(product)

        val iso22Only = shouldThrow<IllegalStateException> { service.delete(label, false) }
        iso22Only.message shouldBe "Height (cm) for 09070601 er brukt av 1 produkter!"

        coEvery {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode, label.label, label.unit, IsoVersion.v16)
        } returns listOf(product)
        val shared = shouldThrow<IllegalStateException> { service.delete(label, false) }
        shared.message shouldBe iso22Only.message
        coVerify(exactly = 0) { repository.delete(any<TechLabelRegistration>()) }
    }

    @Test
    fun `unused label can be deleted after checking both versions`() = runBlocking {
        coEvery {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(any(), any(), any(), any())
        } returns emptyList()

        service.delete(label, false)

        coVerify(exactly = 1) {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode, label.label, label.unit, IsoVersion.v16)
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode22!!, label.label, label.unit, IsoVersion.v22)
            repository.delete(label)
        }
    }

    @Test
    fun `rename migrates both previous category matches once and preserves values`() = runBlocking {
        fun variant(value: String) = ProductRegistration(
            id = UUID.randomUUID(), seriesUUID = UUID.randomUUID(), supplierId = UUID.randomUUID(),
            supplierRef = UUID.randomUUID().toString(), articleName = "Variant", hmsArtNr = null,
            productData = ProductData(techData = listOf(
                TechData(key = label.label, value = value, unit = "cm"),
                TechData(key = "Weight", value = "5", unit = "kg"),
            )),
        )
        val iso16Only = variant("10")
        val iso22Only = variant("20")
        val shared = variant("30")
        val renamed = label.copy(label = "New height", unit = "mm", isoCode = "11111111", isoCode22 = "22222222")
        coEvery {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode, label.label, label.unit, IsoVersion.v16)
        } returns listOf(iso16Only, shared)
        coEvery {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode22!!, label.label, label.unit, IsoVersion.v22)
        } returns listOf(iso22Only, shared)
        coEvery { products.saveAndCreateEventIfNotDraftAndApproved(any(), true) } answers { firstArg() }

        service.changeProductsTechDataWithTechLabel(label, renamed)

        listOf(iso16Only, iso22Only, shared).forEach { original ->
            coVerify(exactly = 1) {
                products.saveAndCreateEventIfNotDraftAndApproved(match {
                    it.id == original.id && it.productData.techData == listOf(
                        TechData(key = "Weight", value = "5", unit = "kg"),
                        TechData(key = renamed.label, value = original.productData.techData.first().value, unit = "mm"),
                    )
                }, true)
            }
        }
        coVerify(exactly = 3) { products.saveAndCreateEventIfNotDraftAndApproved(any(), true) }
    }

    @Test
    fun `rename without previous ISO22 code still migrates ISO16 products`() = runBlocking {
        val unmapped = label.copy(isoCode22 = null)
        val original = ProductRegistration(
            id = UUID.randomUUID(), seriesUUID = UUID.randomUUID(), supplierId = UUID.randomUUID(),
            supplierRef = "variant", articleName = "Variant", hmsArtNr = null,
            productData = ProductData(techData = listOf(TechData(key = label.label, value = "42", unit = "cm"))),
        )
        coEvery {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(label.isoCode, label.label, label.unit, IsoVersion.v16)
        } returns listOf(original)
        coEvery { products.saveAndCreateEventIfNotDraftAndApproved(any(), true) } answers { firstArg() }

        service.changeProductsTechDataWithTechLabel(unmapped, label.copy(label = "New height"))

        coVerify(exactly = 1) {
            products.saveAndCreateEventIfNotDraftAndApproved(match {
                it.productData.techData == listOf(TechData(key = "New height", value = "42", unit = "cm"))
            }, true)
        }
        coVerify(exactly = 0) {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(any(), any(), any(), IsoVersion.v22)
        }
    }

    @Test
    fun `forced delete skips usage checks`() = runBlocking {
        service.delete(label, true, IsoVersion.v22)

        coVerify(exactly = 0) {
            products.findByStartsWithIsoCategoryAndTechLabelKeyUnit(any(), any(), any(), any())
        }
        coVerify(exactly = 1) { repository.delete(label) }
    }
}
