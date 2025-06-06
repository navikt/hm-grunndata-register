package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.data.model.Pageable
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationRepository
import no.nav.hm.grunndata.register.agreement.toEntity
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import org.junit.jupiter.api.Test

@MicronautTest
class ProductRegistrationRepositoryTest(
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val seriesGroupRepository: SeriesRegistrationRepository,
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val objectMapper: ObjectMapper,
    private val delkontraktRegistrationRepository: DelkontraktRegistrationRepository,
) {
    @Test
    fun crudRepositoryTest() {
        val productData1 =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = "En kort beskrivelse av produktet",
                        text = "En lang beskrivelse av produktet",
                    ),
                techData = listOf(
                    TechData(key = "maksvekt", unit = "kg", value = "120"),
                    TechData(key = "bredde", unit = "cm", value = "120"),
                    TechData(key = "Brukerhøyde maks", unit = "kg", value = "120")
                )
            )
        val productData2 =
            ProductData(
                attributes =
                Attributes(
                    shortdescription = "En kort beskrivelse av produktet",
                    text = "En lang beskrivelse av produktet",
                ),
                techData = listOf(
                    TechData(key = "maksvekt", unit = "kg", value = "120"),
                    TechData(key = "bredde", unit = "cm", value = "120"))
            )
        val supplierId = UUID.randomUUID()
        val seriesUUID = UUID.randomUUID()
        val registration1 =
            ProductRegistration(
                id = UUID.randomUUID(),
                seriesId = "series-321",
                seriesUUID = seriesUUID,
                isoCategory = "12001314",
                supplierId = supplierId,
                title = "Dette er produkt title",
                articleName = "Dette er produkt 1 med og med",
                hmsArtNr = "321 ",
                supplierRef = "eksternref-321 ",
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                productData = productData1,
                updatedByUser = "user",
                createdByUser = "user",
                version = 1,
                accessory = false,
                sparePart = false,
                mainProduct = true
            )
        val registration2 =
            ProductRegistration(
                id = UUID.randomUUID(),
                seriesId = "series-123",
                seriesUUID = seriesUUID,
                isoCategory = "12001314",
                supplierId = supplierId,
                title = "Dette er produkt title",
                articleName = "Dette er produkt 1 med og med",
                hmsArtNr = "123",
                supplierRef = "eksternref-124",
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                productData = productData2,
                updatedByUser = "user",
                createdByUser = "user",
                version = 1,
                accessory = true,
                sparePart = false,
                mainProduct = false
            )
        val agreementId = UUID.randomUUID()
        val postId1 = UUID.randomUUID()
        val postId2 = UUID.randomUUID()

        val delkontraktToSave1 =
            DelkontraktRegistrationDTO(
                id = postId1,
                agreementId = agreementId,
                delkontraktData = DelkontraktData(title = "delkontrakt 1", description = "beskrivelse", sortNr = 1),
                createdBy = "tester",
                updatedBy = "tester",
                identifier = postId1.toString()
            )

        val delkontraktToSave2 =
            DelkontraktRegistrationDTO(
                id = postId2,
                agreementId = agreementId,
                delkontraktData = DelkontraktData(title = "delkontrakt 2", description = "beskrivelse", sortNr = 2),
                createdBy = "tester",
                updatedBy = "tester",
                identifier = postId2.toString()
            )

        val agreement =
            ProductAgreementRegistration(
                agreementId = agreementId,
                hmsArtNr = "123",
                post = 1,
                rank = 1,
                postId = postId1,
                reference = "20-1423",
                productId = registration1.id,
                seriesUuid = registration1.seriesUUID,
                supplierId = supplierId,
                supplierRef = registration1.supplierRef,
                createdBy = "user",
                title = "Test product agreement",
                articleName = "Test article",
                status = ProductAgreementStatus.ACTIVE,
            )
        val agreement2 =
            ProductAgreementRegistration(
                agreementId = agreementId,
                hmsArtNr = "1234",
                post = 2,
                rank = 2,
                postId = postId2,
                reference = "20-1423",
                productId = registration1.id,
                seriesUuid = registration1.seriesUUID,
                supplierId = supplierId,
                supplierRef = registration1.supplierRef,
                createdBy = "user",
                title = "Test product agreement",
                articleName = "Test article",
                status = ProductAgreementStatus.ACTIVE,
            )

        runBlocking {
            val saved1 = productRegistrationRepository.save(registration1)
            val saved2 = productRegistrationRepository.save(registration2)
            saved1.shouldNotBeNull()
            val savedDelkontrakt1 = delkontraktRegistrationRepository.save(delkontraktToSave1.toEntity())
            val savedDelkontrakt2 = delkontraktRegistrationRepository.save(delkontraktToSave2.toEntity())
            val savedAgreement = productAgreementRegistrationRepository.save(agreement)
            val savedAgreement2 = productAgreementRegistrationRepository.save(agreement2)
            val foundAgreement =
                productAgreementRegistrationRepository.findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
                    agreement.supplierId,
                    agreement.supplierRef,
                    agreement.agreementId,
                    agreement.postId,

                )
            foundAgreement.shouldNotBeNull()
            val inDb = productRegistrationRepository.findById(saved1.id)
            inDb.shouldNotBeNull()
            inDb.accessory shouldBe false
            inDb.mainProduct shouldBe true
            inDb.sparePart shouldBe false
            saved1.hmsArtNr shouldBe inDb.hmsArtNr
            val approve = inDb.approve("NAVN1")
            val updated = productRegistrationRepository.update(approve)
            updated.id shouldBe saved1.id
            updated.isApproved() shouldBe true
            updated.isDraft() shouldBe false
            updated.published.shouldNotBeNull()
            updated.seriesUUID shouldBe seriesUUID
            val byHMSArtNr =
                productRegistrationRepository.findByHmsArtNrAndSupplierId(saved1.hmsArtNr!!, saved1.supplierId)
            byHMSArtNr.shouldNotBeNull()
            val seriesGroup = seriesGroupRepository.findSeriesGroup(Pageable.from(0, 10))
            val seriesGroupSupplier = seriesGroupRepository.findSeriesGroup(supplierId, Pageable.UNPAGED)
            println(objectMapper.writeValueAsString(seriesGroupSupplier))
            val lastProductInSeries = productRegistrationRepository.findLastProductInSeries(seriesUUID)
            lastProductInSeries.shouldNotBeNull()
            updated.productData.techData.size shouldBe 3
            val distinct = productRegistrationRepository.findDistinctByProductTechDataJsonQuery("""[{"key":"Brukerhøyde maks","unit":"kg"}]""")
            distinct.size shouldBe 1
            distinct[0].id shouldBe updated.id

            val productByHmsArtNr = productRegistrationRepository.findByHmsArtNrStartingWithAndRegistrationStatusInAndSupplierIdAndMainProduct(
                "321",
                listOf(RegistrationStatus.ACTIVE),
                supplierId
            )
            productByHmsArtNr.shouldNotBeNull()


            val productBySupplierRef = productRegistrationRepository.findBySupplierRefStartingWithAndRegistrationStatusInAndSupplierIdAndMainProduct(
                "eksternref-321",
                listOf(RegistrationStatus.ACTIVE),
                supplierId
            )

            productBySupplierRef.shouldNotBeNull()
        }
    }
}
