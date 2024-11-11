package no.nav.hm.grunndata.register.catalog

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.productagreement.toEntity
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@MicronautTest
class CatalogImportServiceTest(
    private val catalogExcelFileImport: CatalogExcelFileImport,
    private val catalogImportRepository: CatalogImportRepository,
    private val catalogImportService: CatalogImportService
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(CatalogImportServiceTest::class.java)
    }

    @Test
    fun testPrepareCatalogImportResult() {
        runBlocking {
            val firstImport =
                catalogExcelFileImport.importExcelFile(
                    CatalogImportServiceTest::class.java.getResourceAsStream("/productagreement/katalog-GAMMEL.xls")!!
                )
            val createCatalogImportResult =
                catalogImportService.prepareCatalogImportResult(firstImport.map { it.toEntity() })

            createCatalogImportResult.insertedList.size shouldBe 28
            createCatalogImportResult.deactivatedList shouldBe emptyList()
            createCatalogImportResult.updatedList shouldBe emptyList()
            catalogImportService.persistCatalogImportResult(createCatalogImportResult)
            val secondImport =
                catalogExcelFileImport.importExcelFile(
                    CatalogImportServiceTest::class.java.getResourceAsStream("/productagreement/katalog-NY.xlsx")!!
                )
            val mapped = secondImport.map { it.toEntity() }
            val createCatalogImportResult2 = catalogImportService.prepareCatalogImportResult(mapped)
            createCatalogImportResult2.shouldNotBeNull()
            createCatalogImportResult2.insertedList.size shouldBe 13
            createCatalogImportResult2.updatedList.size shouldBe 27
            createCatalogImportResult2.deactivatedList.size shouldBe 1
        }
    }

}