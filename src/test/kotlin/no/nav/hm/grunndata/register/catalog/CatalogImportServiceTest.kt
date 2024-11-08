package no.nav.hm.grunndata.register.catalog

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@MicronautTest
class CatalogImportServiceTest(private val catalogImportExcelFileImport: CatalogExcelFileImport, private val catalogImportRepository: CatalogImportRepository, private val catalogImportService: CatalogImportService) {

    @Test
    fun testCreateCatalogImportResult() = runBlocking {
        val result =
            catalogImportExcelFileImport.importExcelFile(CatalogImportServiceTest::class.java.getResourceAsStream("/productagreement/katalog-NY.xlsx"))
    }

}