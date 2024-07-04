package no.nav.hm.grunndata.register.productagreement

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
                                      private val productAgreementImportExcelService: ProductAgreementImportExcelService) {

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
                val productAgreements = productAgreementImportExcelService.importExcelFile(it!!)
                productAgreements.size shouldBe 6
                productAgreements[4].accessory shouldBe true
                productAgreements[4].sparePart shouldBe false
                productAgreements[5].accessory shouldBe false
                productAgreements[5].sparePart shouldBe true
            }
        }
    }

    @Test
    fun testDelkontraktNrExtract() {
        val regex = "d(\\d+)([A-Z]*)r(\\d*)".toRegex()
        val del1 = "d1r1"
        val del2 = "d1Ar1"
        val del3 = "d1Br99" // mean no rank
        val del4 = "d1r"   // mean no rank
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
    }

    @Test
    fun testFindSimilarTitleNames() {
        val title1 = "Skrittsele John sittevogn Kangoo str6"
        val title2 = "Skrittsele John sittevogn Kangoo str5"
        // count identical words between two strings
        val words1 = title1.split("\\s+".toRegex()).toSet()
        val words2 = title2.split("\\s+".toRegex()).toSet()
        println(words1.intersect(words2).size)
    }

}

