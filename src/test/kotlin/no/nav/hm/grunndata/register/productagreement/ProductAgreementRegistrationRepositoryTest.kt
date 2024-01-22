package no.nav.hm.grunndata.register.productagreement

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.productagreement.ProductAgreementImportExcelService.Companion.EXCEL
import org.junit.jupiter.api.Test
import java.util.*


@MicronautTest
class ProductAgreementRegistrationRepositoryTest(private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository)
{
    @Test
    fun testProductAgreementRegistrationRepository() {
        runBlocking {
            val saved = productAgreementRegistrationRepository.save(
                ProductAgreementRegistration(
                    agreementId = UUID.randomUUID(),
                    hmsArtNr = "1234",
                    post = 1,
                    rank = 1,
                    reference = "20-1423",
                    supplierId = UUID.randomUUID(),
                    supplierRef = "TK1235-213",
                    createdBy = EXCEL,
                    title = "Test product agreement",
                    status = ProductAgreementStatus.ACTIVE,
                    articleName = "Test article"
                )
            )

            saved.shouldNotBeNull()
            val found = productAgreementRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            found.agreementId shouldBe saved.agreementId
            found.createdBy shouldBe EXCEL
            found.title shouldBe "Test product agreement"
            found.status shouldBe ProductAgreementStatus.ACTIVE
        }
    }

}