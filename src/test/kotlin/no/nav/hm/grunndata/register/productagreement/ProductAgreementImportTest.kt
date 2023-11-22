package no.nav.hm.grunndata.register.productagreement

import io.kotest.matchers.shouldBe
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.supplier.toRapidDTO
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductAgreementImportTest(private val supplierRegistrationService: SupplierRegistrationService,
                                 private val productAgreementImportExcelService: ProductAgreementImportExcelService) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportTest::class.java)
    }

    @Test
    fun testImportExcel() {
        runBlocking {
            val supplierId = UUID.randomUUID()
            val testSupplier = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = supplierId,
                    supplierData = SupplierData(
                        address = "address 4",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier4@test.test",
                    ),
                    identifier = "$supplierId-unique-name",
                    name = "Leverandør AS -$supplierId"
                )
            ).toRapidDTO()
            ProductAgreementImportTest::class.java.classLoader.getResourceAsStream("productagreement/katalog-test.xls").use {
                val products = productAgreementImportExcelService.importExcelFile(it!!)
                products.forEach {
                    LOG.info("ProductAgreement: $it")
                }
                products.size shouldBe 3
            }
        }
    }

}

