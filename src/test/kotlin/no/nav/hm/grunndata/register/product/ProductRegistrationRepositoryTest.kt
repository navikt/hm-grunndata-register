package no.nav.hm.grunndata.register.product

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class ProductRegistrationRepositoryTest(private val productRegistrationRepository: ProductRegistrationRepository) {

    @Test
    fun crudRepositoryTest() {
        val productDTO = ProductDTO (
            supplierId = UUID.randomUUID(),
            title = "Dette er produkt title",
            attributes = mapOf(
                Pair("articlename", "produktnavn"), Pair("shortdescription", "En kort beskrivelse av produktet"),
                Pair("text", "En lang beskrivelse av produktet")
            ),
            HMSArtNr = "123",
            identifier = "hmdb-123",
            supplierRef = "eksternref-123",
            isoCategory = "12001314",
            accessory = false,
            sparePart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(Media(uri="https://ekstern.url/123.jpg", text = "bilde av produktet", source = MediaSourceType.EXTERNALURL)),
            agreementInfo = AgreementInfo(id = 1, identifier = "hmdbid-1", rank = 1, postId = 123, postNr = 1, reference = "AV-142")
        )
        val registration = ProductRegistration (
            id = productDTO.id,
            supplierId = UUID.randomUUID(),
            supplierRef = productDTO.supplierRef,
            HMSArtNr = productDTO.HMSArtNr ,
            title = productDTO.title,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.NOT_APPROVED,
            status  = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            productDTO = productDTO,
            updatedByUser = "user",
            createdByUser = "user",
            version = 1
        )
        runBlocking {
            val saved  = productRegistrationRepository.save(registration)
            saved.shouldNotBeNull()
            val inDb = productRegistrationRepository.findById(saved.id)
            inDb.shouldNotBeNull()
            saved.HMSArtNr shouldBe inDb.HMSArtNr
            val approve = inDb.approve("NAVN1")
            val updated = productRegistrationRepository.update(approve)
            updated.id shouldBe saved.id
            updated.isApproved() shouldBe true
            updated.isDraft() shouldBe false
            updated.published.shouldNotBeNull()
            val byHMSArtNr = productRegistrationRepository.findByHMSArtNrAndSupplierId(saved.HMSArtNr!!, saved.supplierId)
            byHMSArtNr.shouldNotBeNull()
        }
    }
}