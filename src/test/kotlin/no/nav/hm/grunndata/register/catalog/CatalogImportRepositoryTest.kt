package no.nav.hm.grunndata.register.catalog

import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@MicronautTest
class CatalogImportRepositoryTest(private val catalogImportRepository: CatalogImportRepository) {

    @Test
    fun testRepository() {
        val testCatalog1 = CatalogImport(
            agreementAction = "agreementAction",
            orderRef = "orderRef",
            hmsArtNr = "hmsArtNr",
            iso = "iso",
            title = "title",
            supplierRef = "supplierRef",
            reference = "reference",
            postNr = "postNr",
            dateFrom = "dateFrom",
            dateTo = "dateTo",
            articleAction = "articleAction",
            articleType = "articleType",
            functionalChange = "functionalChange",
            forChildren = "forChildren",
            supplierName = "supplierName",
            supplierCity = "supplierCity",
            mainProduct = true,
            sparePart = true,
            accessory = true
        )
        val testCatalog2 = CatalogImport(
            agreementAction = "agreementAction",
            orderRef = "orderRef",
            hmsArtNr = "hmsArtNr",
            iso = "iso",
            title = "title",
            supplierRef = "supplierRef",
            reference = "reference",
            postNr = "postNr",
            dateFrom = "dateFrom",
            dateTo = "dateTo",
            articleAction = "articleAction",
            articleType = "articleType",
            functionalChange = "functionalChange",
            forChildren = "forChildren",
            supplierName = "supplierName",
            supplierCity = "supplierCity",
            mainProduct = true,
            sparePart = true,
            accessory = true
        )
        runBlocking {
            val saved1 = catalogImportRepository.save(testCatalog1)
            val saved2 = catalogImportRepository.save(testCatalog2)
            saved1.id shouldBe testCatalog1.id
            saved2.id shouldBe testCatalog2.id
            val found = catalogImportRepository.findByOrderRef("orderRef")
            found.size shouldBe 2
        }
    }
}