package no.nav.hm.grunndata.register.archive

import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistration
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationRepository
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import org.junit.jupiter.api.Test
import java.util.UUID

@MicronautTest
class ArchiveServiceTest(
    private val archiveService: ArchiveService,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository,
    private val archiveRepository: ArchiveRepository,
) {

    val productId1 = UUID.randomUUID()
    val productId2 = UUID.randomUUID()

    init {
        val supplierId = UUID.randomUUID()
        val hmsArtNr1 = "1111"
        val hmsArtNr2 = "2222"
        val supplierRef1 = "supplierRef-1"
        val supplierRef2 = "supplierRef-2"
        val agreementId = UUID.randomUUID()

        val product1 = ProductRegistration(
            id = productId1,
            seriesUUID = UUID.randomUUID(),
            supplierId = supplierId,
            supplierRef = supplierRef1,
            hmsArtNr = hmsArtNr1,
            articleName = "Test Product",
            productData = ProductData(),
            title = "Test Product",
            registrationStatus = RegistrationStatus.ACTIVE,
            adminStatus = AdminStatus.APPROVED,
            draftStatus = DraftStatus.DONE,
            createdBy = "testUser",
            updatedBy = "testUser",
        )
        val product2 = ProductRegistration(
            id = productId2,
            seriesUUID = UUID.randomUUID(),
            supplierId = supplierId,
            supplierRef = supplierRef2,
            hmsArtNr = hmsArtNr2,
            articleName = "Test Product 2",
            productData = ProductData(),
            title = "Test Product 2",
            registrationStatus = RegistrationStatus.DELETED,
            adminStatus = AdminStatus.APPROVED,
            draftStatus = DraftStatus.DONE,
            createdBy = "testUser",
            updatedBy = "testUser"
        )
        val delKontrakt = DelkontraktRegistration(
            agreementId = agreementId,
            delkontraktData = DelkontraktData()
        )

        val productAgreement = ProductAgreementRegistration(
            title = "Test Agreement",
            articleName = "Test Agreement Product",
            supplierId = supplierId,
            supplierRef = supplierRef1,
            productId = productId1,
            hmsArtNr = hmsArtNr1,
            agreementId = agreementId,
            reference = "agreement-ref-1",
            post = 1,
            rank = 1,
            postId = delKontrakt.id,
            createdBy = "testUser",
        )

        val productAgreement2 = ProductAgreementRegistration(
            title = "Test Agreement 2",
            articleName = "Test Agreement Product 2",
            status = ProductAgreementStatus.DELETED,
            supplierId = supplierId,
            supplierRef = supplierRef2,
            productId = productId2,
            hmsArtNr = hmsArtNr2,
            agreementId = agreementId,
            reference = "agreement-ref-1",
            post = 1,
            rank = 1,
            postId = delKontrakt.id,
            createdBy = "testUser",
        )
        runBlocking {
        productRegistrationRepository.save(product1)
        productRegistrationRepository.save(product2)
        delkontraktRegistrationRepository.save(delKontrakt)
        productAgreementRegistrationRepository.save(productAgreement)
        productAgreementRegistrationRepository.save(productAgreement2)
        }
    }

    @Test
    fun archiveServiceTest() {
        runBlocking {
            archiveService.getAllHandlers().size shouldBe 2
            archiveService.archiveAll()
            archiveRepository.findByOid(productId1).size shouldBe 0
            val archived = archiveRepository.findByOid(productId2)
            archived.size shouldBe 1
            archiveRepository.update(archived[0].copy(status = ArchiveStatus.UNARCHIVE))
            archiveService.unarchiveAll()
            archiveRepository.findByOid(productId2).size shouldBe 1
        }
    }
}