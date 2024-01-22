package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.data.model.Pageable
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class ProductRegistrationRepositoryTest(private val productRegistrationRepository: ProductRegistrationRepository,
                                        private val seriesGroupRepository: SeriesRegistrationRepository,
                                        private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
                                        private val objectMapper: ObjectMapper) {

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
            media = setOf(
                MediaInfo(uri="123.jpg", text = "bilde av produktet", source = MediaSourceType.EXTERNALURL,
                sourceUri = "https://ekstern.url/123.jpg"),
                MediaInfo(uri="124.jpg", text = "bilde av produktet 2", source = MediaSourceType.EXTERNALURL,
                    sourceUri = "https://ekstern.url/124.jpg")
            )
        )
        val supplierId =  UUID.randomUUID()
        val seriesUUID = UUID.randomUUID()
        val registration = ProductRegistration (
            id = UUID.randomUUID(),
            seriesId = "series-123",
            seriesUUID = seriesUUID,
            isoCategory = "12001314",
            supplierId = supplierId,
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
        val agreementId = UUID.randomUUID()
        val agreement = ProductAgreementRegistration(
            agreementId = agreementId,
            hmsArtNr = "123",
            post = 1,
            rank = 1,
            reference = "20-1423",
            productId = registration.id,
            seriesId = registration.seriesUUID,
            supplierId = supplierId,
            supplierRef = registration.supplierRef,
            createdBy = "user",
            title = "Test product agreement",
            articleName = "Test article",
            status = ProductAgreementStatus.ACTIVE
        )
        val agreement2 = ProductAgreementRegistration(
            agreementId = agreementId,
            hmsArtNr = "1234",
            post = 2,
            rank = 2,
            reference = "20-1423",
            productId = registration.id,
            seriesId = registration.seriesUUID,
            supplierId = supplierId,
            supplierRef = registration.supplierRef,
            createdBy = "user",
            title = "Test product agreement",
            articleName = "Test article",
            status = ProductAgreementStatus.ACTIVE
        )
        val agreement3 = ProductAgreementRegistration(
            agreementId = agreementId,
            hmsArtNr = "12345",
            post = 3,
            rank = 3,
            reference = "20-1423",
            productId = UUID.randomUUID(),
            seriesId = UUID.randomUUID(),
            supplierId = supplierId,
            supplierRef = "eksternref-1234",
            createdBy = "user",
            title = "Test product agreement",
            articleName = "Test article",
            status = ProductAgreementStatus.ACTIVE
        )
        runBlocking {
            val savedAgreement = productAgreementRegistrationRepository.save(agreement)
            val savedAgreement2 = productAgreementRegistrationRepository.save(agreement2)
            val savedAgreement3 = productAgreementRegistrationRepository.save(agreement3)
            val foundAgreement = productAgreementRegistrationRepository.findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
                agreement.supplierId, agreement.supplierRef, agreement.agreementId, agreement.post, agreement.rank)
            foundAgreement.shouldNotBeNull()
            val saved  = productRegistrationRepository.save(registration)
            saved.shouldNotBeNull()
            val inDb = productRegistrationRepository.findById(saved.id)
            inDb.shouldNotBeNull()
            inDb.agreements.size shouldBe 2
            saved.hmsArtNr shouldBe inDb.hmsArtNr
            val approve = inDb.approve("NAVN1")
            val updated = productRegistrationRepository.update(approve)
            updated.id shouldBe saved.id
            updated.isApproved() shouldBe true
            updated.isDraft() shouldBe false
            updated.published.shouldNotBeNull()
            updated.productData.media.size shouldBe 2
            updated.seriesUUID shouldBe seriesUUID
            val byHMSArtNr = productRegistrationRepository.findByHmsArtNrAndSupplierId(saved.hmsArtNr!!, saved.supplierId)
            byHMSArtNr.shouldNotBeNull()
            val seriesGroup = seriesGroupRepository.findSeriesGroup(Pageable.from(0, 10))
            val seriesGroupSupplier = seriesGroupRepository.findSeriesGroup(supplierId, Pageable.UNPAGED)
            println(objectMapper.writeValueAsString(seriesGroupSupplier))

        }
    }
}
