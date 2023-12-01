package no.nav.hm.grunndata.register.productagreement

import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementData
import no.nav.hm.grunndata.register.agreement.AgreementRegistration
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.agreement.toDTO
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.supplier.toRapidDTO
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductAgreementExcelImportTest(private val supplierRegistrationService: SupplierRegistrationService,
                                      private val agreementRegistrationService: AgreementRegistrationService,
                                      private val productAgreementImportExcelService: ProductAgreementImportExcelService,
                                      private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

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
            val data = AgreementData(
                text = "some text", resume = "resume",
                identifier = UUID.randomUUID().toString(),
                posts = listOf(
                    AgreementPost(identifier = "unik-post1", title = "Post title",
                        description = "post description", nr = 1), AgreementPost(identifier = "unik-post2", title = "Post title 2",
                        description = "post description 2", nr = 2)
                ))
            val agreementRegistration = AgreementRegistration(
                id = agreementId, published = agreement.published, expired = agreement.expired, title = agreement.title,
                reference = agreement.reference, updatedByUser = "username", createdByUser = "username", agreementData = data
            )
            agreementRegistrationService.save(agreementRegistration.toDTO())
            ProductAgreementExcelImportTest::class.java.classLoader.getResourceAsStream("productagreement/katalog-test.xls").use {
                val products = productAgreementImportExcelService.importExcelFile(it!!)
                products.forEach {
                    LOG.info("ProductAgreement: $it")
                }
                val saved = productAgreementRegistrationService.saveAll(products)
                products.size shouldBe saved.size
            }
        }
    }

}

