package no.nav.hm.grunndata.register.product

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import org.junit.jupiter.api.Test

@MicronautTest
class ProductRegistrationVersionRepositoryTest(private val productRegistrationVersionRepository: ProductRegistrationVersionRepository) {

    @Test
    fun crudTest() {
        val productId = UUID.randomUUID()
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val product1 = ProductRegistration(
            id = productId,
            supplierId = supplierId,
            seriesId = seriesId.toString(),
            seriesUUID = seriesId,
            articleName = "articleName",
            hmsArtNr = "hmsArtNr",
            supplierRef = "supplierRef",
            title = "title",
            isoCategory = "12345678",
            productData = ProductData(
                media = setOf(
                    MediaInfoDTO(
                        sourceUri = "sourceUri",
                        filename = "filename",
                        uri = "uri",
                        priority = 1,
                        type = MediaType.IMAGE,
                        text = "text",
                        source = MediaSourceType.REGISTER,
                        updated = LocalDateTime.now(),
                    )
                ),
            ),
            draftStatus = DraftStatus.DONE,
            registrationStatus = RegistrationStatus.ACTIVE,
            adminStatus = AdminStatus.APPROVED,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(15),
            published = LocalDateTime.now(),
            createdBy = "createdBy",
            updatedBy = "updatedBy",
            updatedByUser = "updatedByUser",
            createdByUser = "createdByUser",
            createdByAdmin = false,
            version = 1
        )

        val version1 = ProductRegistrationVersion(
            productId = product1.id,
            status = product1.registrationStatus,
            adminStatus = product1.adminStatus,
            draftStatus = product1.draftStatus,
            productRegistration = product1,
            updated = product1.updated,
            version = product1.version
        )
        runBlocking {
            val saved = productRegistrationVersionRepository.save(version1)
            saved.shouldNotBeNull()
            val found = productRegistrationVersionRepository.findOneByProductIdAndDraftStatusAndAdminStatusOrderByUpdatedDesc(productId, DraftStatus.DONE, AdminStatus.APPROVED)
            found.shouldNotBeNull()
            found.version shouldBe product1.version
        }
    }
}