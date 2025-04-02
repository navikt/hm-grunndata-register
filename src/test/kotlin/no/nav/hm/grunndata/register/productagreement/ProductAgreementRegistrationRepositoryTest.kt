package no.nav.hm.grunndata.register.productagreement

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationRepository
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationService
import no.nav.hm.grunndata.register.agreement.toEntity
import no.nav.hm.grunndata.register.productagreement.ProductAgreementImportExcelService.Companion.EXCEL
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@MicronautTest
class ProductAgreementRegistrationRepositoryTest(
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository,
) {
    @Test
    fun testProductAgreementRegistrationRepository() {
        runBlocking {
            val postId = UUID.randomUUID()
            val agreementId = UUID.randomUUID()

            val supplierId = UUID.randomUUID()

            val delkontraktToSave =
                DelkontraktRegistrationDTO(
                    id = postId,
                    agreementId = agreementId,
                    delkontraktData = DelkontraktData(title = "delkontrakt 1", description = "beskrivelse", sortNr = 1),
                    createdBy = "tester",
                    updatedBy = "tester",
                    identifier = postId.toString()
                )
            delkontraktRegistrationRepository.save(delkontraktToSave.toEntity())
            val saved =
                productAgreementRegistrationRepository.save(
                    ProductAgreementRegistration(
                        agreementId = agreementId,
                        hmsArtNr = "1234",
                        post = 1,
                        rank = 1,
                        postId = postId,
                        reference = "20-1423",
                        supplierId = supplierId,
                        supplierRef = "TK1235-213",
                        createdBy = EXCEL,
                        title = "Test product agreement",
                        status = ProductAgreementStatus.ACTIVE,
                        articleName = "Test article",
                        accessory = false,
                        sparePart = false,
                        mainProduct = true,
                    ),
                )

            saved.shouldNotBeNull()
            val found = productAgreementRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            found.agreementId shouldBe saved.agreementId
            found.createdBy shouldBe EXCEL
            found.updatedBy shouldBe REGISTER
            found.title shouldBe "Test product agreement"
            found.status shouldBe ProductAgreementStatus.ACTIVE
            found.postId shouldBe postId
            found.mainProduct shouldBe true

            // should throw Duplicate key exception
            assertThrows<DataAccessException> {
                productAgreementRegistrationRepository.save(
                    ProductAgreementRegistration(
                        agreementId = agreementId,
                        hmsArtNr = "1234",
                        post = 1,
                        rank = 1,
                        postId = postId,
                        reference = "20-1423",
                        supplierId = supplierId,
                        supplierRef = "TK1235-213",
                        createdBy = EXCEL,
                        title = "Test product agreement",
                        status = ProductAgreementStatus.ACTIVE,
                        articleName = "Test article",
                    ),
                )
            }
        }
    }
}
