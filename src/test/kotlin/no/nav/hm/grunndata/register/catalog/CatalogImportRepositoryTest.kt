package no.nav.hm.grunndata.register.catalog

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.catalog.ProductAgreementImportExcelService.Companion.EXCEL
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.grunndata.register.servicejob.ServiceJob
import no.nav.hm.grunndata.register.servicejob.ServiceJobRepository
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@MicronautTest
class CatalogImportRepositoryTest(
    private val catalogImportRepository: CatalogImportRepository,
    private val serviceJobRepository: ServiceJobRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val productRegistrationRepository: ProductRegistrationRepository,
) {

    @Test
    fun testRepository() {
        runBlocking {
            val agreementId = UUID.randomUUID()
            val supplierId = UUID.randomUUID()

            val testCatalog1 = catalogImportRepository.save(CatalogImport(
                agreementAction = "agreementAction",
                orderRef = "1234",
                hmsArtNr = "432100",
                iso = "iso",
                title = "title",
                supplierRef = "supplierRef1",
                reference = "20-1424",
                postNr = "d1",
                dateFrom = LocalDate.now(),
                dateTo = LocalDate.now(),
                articleAction = "articleAction",
                articleType = "articleType",
                functionalChange = "functionalChange",
                forChildren = "forChildren",
                supplierName = "supplierName",
                supplierCity = "supplierCity",
                mainProduct = false,
                sparePart = true,
                accessory = false,
                agreementId = agreementId,
                supplierId = supplierId

            ))
            val testCatalog2 = catalogImportRepository.save(CatalogImport(
                agreementAction = "agreementAction",
                orderRef = "1234",
                hmsArtNr = "432101",
                iso = "iso",
                title = "title",
                supplierRef = "supplierRef2",
                reference = "20-1424",
                postNr = "d2",
                dateFrom = LocalDate.now(),
                dateTo = LocalDate.now(),
                articleAction = "articleAction",
                articleType = "articleType",
                functionalChange = "functionalChange",
                forChildren = "forChildren",
                supplierName = "supplierName",
                supplierCity = "supplierCity",
                mainProduct = false,
                sparePart = false,
                accessory = true,
                agreementId = agreementId,
                supplierId = supplierId
            ))
            val testCatalog3 = CatalogImport(
                agreementAction = "agreementAction",
                orderRef = "1234",
                hmsArtNr = "432101",
                iso = "iso",
                title = "title",
                supplierRef = "supplierRef2",
                reference = "20-1424",
                postNr = "d2",
                dateFrom = LocalDate.now(),
                dateTo = LocalDate.now(),
                articleAction = "articleAction",
                articleType = "articleType",
                functionalChange = "functionalChange",
                forChildren = "forChildren",
                supplierName = "supplierName",
                supplierCity = "supplierCity",
                mainProduct = false,
                sparePart = false,
                accessory = true,
                agreementId = agreementId,
                supplierId = supplierId
            )
            testCatalog1 shouldNotBeEqual testCatalog2
            testCatalog2 shouldBeEqual  testCatalog3

            val seriesId = UUID.randomUUID()
            val product1 = productRegistrationRepository.save(
                ProductRegistration(
                    hmsArtNr = "432100",
                    supplierId = supplierId,
                    supplierRef = "supplierRef1",
                    createdBy = EXCEL,
                    title = "Test product agreement",
                    articleName = "Test article",
                    id = UUID.randomUUID(),
                    seriesUUID = seriesId,
                    productData = ProductData(),
                )
            )
            val product2 = productRegistrationRepository.save(ProductRegistration(
                hmsArtNr = "432101",
                supplierId = supplierId,
                supplierRef = "supplierRef2",
                createdBy = EXCEL,
                title = "Test product agreement 2",
                articleName = "supplierRef2",
                id = UUID.randomUUID(),
                seriesUUID = seriesId,
                productData = ProductData(),
            ))

            val seriesRegistration = seriesRegistrationRepository.save(SeriesRegistration(
                id = seriesId,
                supplierId = supplierId,
                identifier = "HMDB-123",
                title = "Series 1",
                text = "Series 1 text",
                isoCategory = "12343212",
                status = SeriesStatus.ACTIVE,
                adminStatus = AdminStatus.PENDING,
                seriesData = SeriesDataDTO(
                    media = setOf(
                        MediaInfoDTO(
                            uri = "http://example.com",
                            type = MediaType.IMAGE,
                            text = "image description",
                            sourceUri = "http://example.com",
                            source = MediaSourceType.REGISTER
                        )
                    )
                )
            ))
            val catalogSeriesInfo = catalogImportRepository.findCatalogProductSeriesInfoByOrderRef("1234")
            catalogSeriesInfo.size shouldBe 2
            catalogSeriesInfo[0].seriesId shouldBe seriesId
            catalogSeriesInfo[0].seriesTitle shouldBe "Series 1"
            catalogSeriesInfo[0].mainProduct shouldBe false
            catalogSeriesInfo[0].sparePart shouldBe true
            catalogSeriesInfo[0].agreementId shouldBe agreementId
        }
    }

    @Test
    fun testCatalogServiceJobInfo() {
        runBlocking {
            val agreementId = UUID.randomUUID()
            val supplierId = UUID.randomUUID()

            // Save CatalogImport
            catalogImportRepository.save(
                CatalogImport(
                    agreementAction = "agreementAction",
                    orderRef = "ORDER-REF-1",
                    hmsArtNr = "555555",
                    iso = "123456",
                    title = "Service Job Title",
                    supplierRef = "supplierRef1",
                    reference = "ref",
                    postNr = "post",
                    dateFrom = LocalDate.now(),
                    dateTo = LocalDate.now(),
                    articleAction = "action",
                    articleType = "HMS Servicetjeneste",
                    functionalChange = "change",
                    forChildren = "children",
                    supplierName = "Supplier",
                    supplierCity = "City",
                    mainProduct = false,
                    sparePart = false,
                    accessory = false,
                    agreementId = agreementId,
                    supplierId = supplierId
                )
            )

            // Save ServiceJob
            val serviceJob = serviceJobRepository.save(
                ServiceJob(
                    id = UUID.randomUUID(),
                    supplierId = supplierId,
                    supplierRef = "supplierRef1",
                    hmsArtNr = "555555",
                    title = "Service Job Title",
                    created = LocalDateTime.now(),
                    updated = LocalDateTime.now(),
                    isoCategory = "123456",
                    published = LocalDateTime.now(),
                    expired = LocalDateTime.now()
                )
            )

            // Test CatalogServiceJobInfo
            val infos = catalogImportRepository.findCatalogServiceJobInfoByOrderRef("ORDER-REF-1")
            infos.size shouldBe 1
            val info = infos.first()
            info.hmsArtNr shouldBe "555555"
            info.title shouldBe "Service Job Title"
            info.serviceId shouldBe serviceJob.id
            info.agreementId shouldBe agreementId
        }
    }
}