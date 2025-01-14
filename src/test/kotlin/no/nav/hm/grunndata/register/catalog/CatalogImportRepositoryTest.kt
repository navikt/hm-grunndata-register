package no.nav.hm.grunndata.register.catalog

import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistration
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationRepository
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementImportExcelService.Companion.EXCEL
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import org.junit.jupiter.api.Test

@MicronautTest
class CatalogImportRepositoryTest(
    private val catalogImportRepository: CatalogImportRepository,
    private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository,
    private val seriesRegistrationRepository: SeriesRegistrationRepository,
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository
) {

    @Test
    fun testRepository() {
        runBlocking {
            val postId1 = UUID.randomUUID()
            val postId2 = UUID.randomUUID()
            val agreementId = UUID.randomUUID()
            val delkontraktRegistration1 = delkontraktRegistrationRepository.save(DelkontraktRegistration(
                id = postId1,
                agreementId = agreementId,
                delkontraktData = DelkontraktData(title = "delkontrakt 1", description = "beskrivelse", sortNr = 1),
                createdBy = "tester",
                updatedBy = "tester",
                identifier = postId1.toString()
            ))
            val delkontraktRegistration2 = delkontraktRegistrationRepository.save(DelkontraktRegistration(
                id = postId2,
                agreementId = agreementId,
                delkontraktData = DelkontraktData(title = "delkontrakt 2", description = "beskrivelse", sortNr = 1),
                createdBy = "tester",
                updatedBy = "tester",
                identifier = postId2.toString()
            ))
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
                accessory = false
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
                accessory = true
            ))
            val supplierId = UUID.randomUUID()
            val seriesId = UUID.randomUUID()
            val productAgreement1 = productAgreementRegistrationRepository.save(ProductAgreementRegistration(
                agreementId = agreementId,
                hmsArtNr = "432100",
                post = 1,
                rank = 1,
                postId = postId1,
                reference = "20-1424",
                supplierId = supplierId,
                supplierRef = "TK1235-213",
                createdBy = EXCEL,
                title = "Test product agreement",
                status = ProductAgreementStatus.ACTIVE,
                articleName = "Test article",
                seriesUuid = seriesId
            ))
            val productAgreement2 = productAgreementRegistrationRepository.save(ProductAgreementRegistration(
                agreementId = UUID.randomUUID(),
                hmsArtNr = "432101",
                post = 2,
                rank = 1,
                postId = postId2,
                reference = "20-1424",
                supplierId = supplierId,
                supplierRef = "TK1235-214",
                createdBy = EXCEL,
                title = "Test product agreement 2",
                status = ProductAgreementStatus.ACTIVE,
                articleName = "Test article 2",
                seriesUuid = seriesId
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
            val catalogSeriesInfo = catalogImportRepository.findCatalogSeriesInfoByOrderRef("1234")
            catalogSeriesInfo.size shouldBe 2
            catalogSeriesInfo[0].seriesId shouldBe seriesId
            catalogSeriesInfo[0].seriesTitle shouldBe "Series 1"
            catalogSeriesInfo[0].mainProduct shouldBe false
        }
    }
}