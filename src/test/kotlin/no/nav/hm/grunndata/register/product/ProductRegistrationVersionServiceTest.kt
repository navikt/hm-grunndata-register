package no.nav.hm.grunndata.register.product

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.product.version.ProductRegistrationVersion
import no.nav.hm.grunndata.register.product.version.ProductRegistrationVersionService
import no.nav.hm.grunndata.register.version.DiffStatus
import no.nav.hm.grunndata.register.version.Difference
import org.junit.jupiter.api.Test

@MicronautTest
class ProductRegistrationVersionServiceTest(private val productRegistrationVersionService: ProductRegistrationVersionService,
                                            private val productRegistrationRepository: ProductRegistrationRepository) {

    @Test
    fun testVersionDifference() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val pending = ProductRegistration(
            seriesUUID = seriesId,
            version = 1,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            articleName = "articleName",
            productData = ProductData(
                attributes = Attributes(
                ),
                techData = listOf(TechData(
                    key = "vekt",
                    value = "1",
                    unit = "kg",
                )),
            ),
            supplierId = supplierId,
            seriesId = seriesId.toString(),
            hmsArtNr = "hmsArtNr123",
            id = productId,
            supplierRef = "supplierRef",
        )

        runBlocking {
            val saved = productRegistrationRepository.save(pending)
            saved.shouldNotBeNull()
            val approved = productRegistrationRepository.update(saved.copy(
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.APPROVED,
                articleName = "articleName2",
                productData = ProductData(
                    attributes = Attributes(
                        shortdescription = "en gammel beskrivelse"
                    ),
                    techData = listOf(TechData(
                        key = "vekt",
                        value = "2",
                        unit = "kg",
                    ), TechData(
                        key = "lengde",
                        value = "3",
                        unit = "m",
                    )),
                )
            ))
            approved.shouldNotBeNull()
            val difference: Difference<String, Any> = productRegistrationVersionService.diffVersions(saved, approved)
            difference.status shouldBe DiffStatus.DIFF
            val pending = productRegistrationRepository.update(
                approved.copy(
                    draftStatus = DraftStatus.DONE,
                    adminStatus = AdminStatus.PENDING,
                    articleName = "Venter på godkjenning",
                    productData = ProductData(
                        attributes = Attributes(
                            shortdescription = "en ny beskrivelse",
                            bestillingsordning = true
                        ),
                        techData = listOf(TechData(
                            key = "vekt",
                            value = "2",
                            unit = "kg",
                        ), TechData(
                            key = "lengde",
                            value = "4",
                            unit = "m",
                        ), TechData(
                            key = "bredde",
                            value = "4",
                            unit = "m",
                        )),
                    )
                )
            )
            val pendingVersion = productRegistrationVersionService.save(
                ProductRegistrationVersion(
                productId = pending.id,
                status = pending.registrationStatus,
                adminStatus = pending.adminStatus,
                draftStatus = pending.draftStatus,
                productRegistration = pending,
                updatedBy = pending.updatedBy,
                updated = pending.updated
                )
            )
            val diffSinceLastApproved: Difference<String, Any> = productRegistrationVersionService.diffWithLastApprovedVersion(pendingVersion)
            diffSinceLastApproved.status shouldBe DiffStatus.DIFF
            diffSinceLastApproved.diff.entriesDiffering.size shouldBe 3
            diffSinceLastApproved.diff.entriesOnlyOnLeft.size shouldBe 1
            diffSinceLastApproved.diff.entriesOnlyOnRight.size shouldBe 0
            diffSinceLastApproved.diff.entriesDiffering["attributes.shortdescription"] shouldBe Pair("en ny beskrivelse", "en gammel beskrivelse")
            println(diffSinceLastApproved)
        }
    }
}