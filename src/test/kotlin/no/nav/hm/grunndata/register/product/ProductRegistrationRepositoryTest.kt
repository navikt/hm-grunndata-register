package no.nav.hm.grunndata.register.product

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductRegistrationRepositoryTest(private val productRegistrationRepository: ProductRegistrationRepository) {

    @Test
    fun crudRepositoryTest() {
        val productDTO = ProductDTO (
            id = UUID.randomUUID(),
            supplierId = UUID.randomUUID(),
            title = "Dette er produkt title",
            attributes = mapOf(
               AttributeNames.articlename to  "produktnavn", AttributeNames.shortdescription to "En kort beskrivelse av produktet",
              AttributeNames.text to "En lang beskrivelse av produktet"
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
            agreementInfo = AgreementInfo(id = UUID.randomUUID(), identifier = "hmdbid-1", rank = 1, postNr = 1,
                reference = "AV-142", expired = LocalDateTime.now())
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