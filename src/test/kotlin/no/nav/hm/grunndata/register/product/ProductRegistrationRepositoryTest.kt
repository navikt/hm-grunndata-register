package no.nav.hm.grunndata.register.product

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.*
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.bouncycastle.asn1.x500.style.RFC4519Style.title
import java.awt.SystemColor.text
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductRegistrationRepositoryTest(private val productRegistrationRepository: ProductRegistrationRepository) {

    @Test
    fun crudRepositoryTest() {
        val productData = ProductData (
            attributes = Attributes (
                shortdescription = "En kort beskrivelse av produktet",
                text = "En lang beskrivelse av produktet"
            ),
            accessory = false,
            sparePart = false,
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(MediaInfo(uri="123.jpg", text = "bilde av produktet", source = MediaSourceType.EXTERNALURL,
                sourceUri = "https://ekstern.url/123.jpg")),
            agreementInfo = AgreementInfo(id = UUID.randomUUID(), identifier = "hmdbid-1", rank = 1, postNr = 1,
                reference = "AV-142", expired = LocalDateTime.now())
        )
        val registration = ProductRegistration (
            id = UUID.randomUUID(),
            seriesId = "series-123",
            isoCategory = "12001314",
            supplierId = UUID.randomUUID(),
            title = "Dette er produkt title",
            articleName = "Dette er produkt 1 med og med",
            hmsArtNr = "123",
            supplierRef = "eksternref-123",
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus  = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            productData = productData,
            updatedByUser = "user",
            createdByUser = "user",
            version = 1
        )
        runBlocking {
            val saved  = productRegistrationRepository.save(registration)
            saved.shouldNotBeNull()
            val inDb = productRegistrationRepository.findById(saved.id)
            inDb.shouldNotBeNull()
            saved.hmsArtNr shouldBe inDb.hmsArtNr
            val approve = inDb.approve("NAVN1")
            val updated = productRegistrationRepository.update(approve)
            updated.id shouldBe saved.id
            updated.isApproved() shouldBe true
            updated.isDraft() shouldBe false
            updated.published.shouldNotBeNull()
            val byHMSArtNr = productRegistrationRepository.findByHmsArtNrAndSupplierId(saved.hmsArtNr!!, saved.supplierId)
            byHMSArtNr.shouldNotBeNull()
        }
    }
}
