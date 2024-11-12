package no.nav.hm.grunndata.register.productagreement

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementData
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistration
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationRepository
import no.nav.hm.grunndata.register.catalog.CatalogExcelFileImport
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@MicronautTest
class ProductAgreementExcelImportTest(private val supplierRegistrationService: SupplierRegistrationService,
                                      private val agreementRegistrationService: AgreementRegistrationService,
                                      private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository,
                                      private val productAgreementImportExcelService: ProductAgreementImportExcelService,
                                      private val accessoryPartHandler: ProductAccessorySparePartAgreementHandler,
                                      private val objectMapper: ObjectMapper,
                                      private val catalogExcelFileImport: CatalogExcelFileImport) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)


    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementExcelImportTest::class.java)
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
            val agreementId = UUID.randomUUID()
            val agreement = AgreementDTO(id = agreementId, published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(2), title = "Title of agreement",
                text = "some text", reference = "22-7601", identifier = "unik-ref1234", resume = "resume",
                posts = listOf(
                    AgreementPost(identifier = "unik-post1", title = "Post title",
                        description = "post description", nr = 1), AgreementPost(identifier = "unik-post2", title = "Post title 2",
                        description = "post description 2", nr = 2)
                ), createdBy = REGISTER, updatedBy = REGISTER,
                created = LocalDateTime.now(), updated = LocalDateTime.now())
            val delkontrakt1 = DelkontraktRegistration(
                id = UUID.randomUUID(),
                agreementId = agreementId,
                delkontraktData = DelkontraktData(
                    title = "1: Delkontrakt",
                    description = "Delkontrakt 1 description",
                    sortNr = 1,
                    refNr = "1"
                ),
                createdBy = REGISTER,
                updatedBy = REGISTER
            )

            val delkontrakt2 = DelkontraktRegistration(
                id = UUID.randomUUID(),
                agreementId = agreementId,
                delkontraktData = DelkontraktData(
                    title = "2: Delkontrakt",
                    description = "Delkontrakt 2 description",
                    sortNr = 2,
                    refNr = "2"
                ),
                createdBy = REGISTER,
                updatedBy = REGISTER
            )

            val delkontrakt1A = DelkontraktRegistration(
                id = UUID.randomUUID(),
                agreementId = agreementId,
                delkontraktData = DelkontraktData(
                    title = "1A: Delkontrakt",
                    description = "Delkontrakt 1A description",
                    sortNr = 3,
                    refNr = "1A"
                ),
                createdBy = REGISTER,
                updatedBy = REGISTER
            )


            val delkontrakt1B = DelkontraktRegistration(
                id = UUID.randomUUID(),
                agreementId = agreementId,
                delkontraktData = DelkontraktData(
                    title = "1B: Delkontrakt ",
                    description = "Delkontrakt 1B description",
                    sortNr = 3,
                    refNr = "1B"
                ),
                createdBy = REGISTER,
                updatedBy = REGISTER
            )

            val data = AgreementData(
                text = "some text", resume = "resume",
                identifier = UUID.randomUUID().toString())

            val agreementRegistration = AgreementRegistrationDTO(
                id = agreementId, published = agreement.published, expired = agreement.expired, title = agreement.title,
                reference = agreement.reference, updatedByUser = "username", createdByUser = "username", agreementData = data
            )

            agreementRegistrationService.save(agreementRegistration)
            delkontraktRegistrationRepository.save(delkontrakt1)
            delkontraktRegistrationRepository.save(delkontrakt2)
            delkontraktRegistrationRepository.save(delkontrakt1A)
            delkontraktRegistrationRepository.save(delkontrakt1B)

            ProductAgreementExcelImportTest::class.java.classLoader.getResourceAsStream("productagreement/katalog-test.xls").use {
//                val productExcelList = catalogExcelFileImport.importExcelFile(it!!)
//                val productAgreementRegistrationList = productAgreementImportExcelService.mapCatalogImport(productExcelList, null)
//                val excelImportedResult = ExcelImportedResult(productExcelList, productAgreementRegistrationList)
//                productExcelList.size shouldBe 7
//                productExcelList[0].bestillingsNr shouldBe "3574253"
//                productExcelList[0].rammeavtaleHandling shouldBe "insert"
//                productAgreementRegistrationList.size shouldBe 8
//                productAgreementRegistrationList[0].accessory shouldBe false
//                productAgreementRegistrationList[0].sparePart shouldBe false
//                productAgreementRegistrationList[4].sparePart shouldBe true
//                productAgreementRegistrationList[4].accessory shouldBe false
//                productAgreementRegistrationList[5].accessory shouldBe false
//                productAgreementRegistrationList[5].sparePart shouldBe true
//                productAgreementRegistrationList[6].isoCategory shouldBe "093390"
//                val productAgreementImportResult = accessoryPartHandler.handleNewProductsInExcelImport(excelImportedResult, null, false)
//                val productAgreementGroupInSeries = productAgreementImportResult.newProductAgreements
//                productAgreementImportResult.newSeries.size shouldBe 4
//                productAgreementImportResult.newAccessoryParts.size shouldBe 5
//                productAgreementImportResult.newProducts.size shouldBe 2
//                productAgreementImportResult.newSeries.forEach{
//                    println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(it))
//                }

            }
        }
    }

    @Test
    fun testDelkontraktNrExtract() {
        val regex = delKontraktRegex
        val del1 = "d1r1"
        val del2 = "d1Ar1"
        val del3 = "d1Br99" // mean no rank
        val del4 = "d1r"   // mean no rank
        val del5 = "d14"   // mean no rank
        val del6 = "d21d22"
        regex.find(del1)?.groupValues?.get(1) shouldBe "1"
        regex.find(del1)?.groupValues?.get(2) shouldBe ""
        regex.find(del1)?.groupValues?.get(3) shouldBe "1"
        regex.find(del2)?.groupValues?.get(1) shouldBe "1"
        regex.find(del2)?.groupValues?.get(2) shouldBe "A"
        regex.find(del2)?.groupValues?.get(3) shouldBe "1"
        regex.find(del3)?.groupValues?.get(1) shouldBe "1"
        regex.find(del3)?.groupValues?.get(2) shouldBe "B"
        regex.find(del3)?.groupValues?.get(3) shouldBe "99"
        regex.find(del4)?.groupValues?.get(1) shouldBe "1"
        regex.find(del4)?.groupValues?.get(2) shouldBe ""
        regex.find(del4)?.groupValues?.get(3) shouldBe ""
        regex.find(del5)?.groupValues?.get(1) shouldBe "14"
        regex.find(del5)?.groupValues?.get(2) shouldBe ""
        regex.find(del5)?.groupValues?.get(3) shouldBe ""
        regex.find(del6)?.groupValues?.get(1) shouldBe "21"
        regex.find(del6)?.groupValues?.get(2) shouldBe ""
    }

}

