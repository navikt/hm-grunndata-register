package no.nav.hm.grunndata.register.part

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.Authentication
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.iso.IsoVersion
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.util.UUID

@MicronautTest
class PartServiceTest(
    private val partService: PartService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun `New part creates series and product`() {
        val supplierId = UUID.randomUUID()
        val authentication = Authentication.build("testperson", mapOf("supplierId" to supplierId.toString()))

        runBlocking {
            val part = partService.createDraftWith(
                authentication, PartDraftWithDTO(
                    title = "Test part",
                    isoCategory = "ISO1234",
                    hmsArtNr = "123456",
                    levArtNr = "654321",
                    sparePart = true,
                    accessory = false,
                    supplierId = supplierId,
                )
            )

            val product = productRegistrationService.findById(part.id)
            val series = seriesRegistrationService.findById(part.seriesUUID)
            product.shouldNotBeNull()
            series.shouldNotBeNull()

            series.mainProduct shouldBe false

            partService.changeToMainProduct(part.seriesUUID, newIsoCode = "12345", authentication)
            val seriesAsMainProduct = seriesRegistrationService.findById(part.seriesUUID)

            seriesAsMainProduct?.mainProduct shouldBe true


        }

    }

    @Test
    fun `ISO22 part draft and conversions retain counterpart when mapping is unavailable`(): Unit = runBlocking {
        val supplierId = UUID.randomUUID()
        val auth = Authentication.build("admin", mapOf("supplierId" to supplierId.toString()))
        val part = partService.createDraftWith(
            auth,
            PartDraftWithDTO(
                title = "ISO22 part", isoCategory = "999990", isoCategory22 = "888890",
                levArtNr = UUID.randomUUID().toString(), sparePart = true, supplierId = supplierId,
            )
        )
        val draftSeries = seriesRegistrationService.findById(part.seriesUUID).shouldNotBeNull()
        draftSeries.isoCategory shouldBe "999990"
        draftSeries.isoCategory22 shouldBe "888890"
        partService.changeToMainProduct(part.seriesUUID, "88888888", auth, IsoVersion.v22)
        val main = seriesRegistrationService.findById(part.seriesUUID).shouldNotBeNull()
        main.mainProduct shouldBe true
        main.isoCategory shouldBe "999990"
        main.isoCategory22 shouldBe "88888888"
        seriesRegistrationService.changeMainProductToPart(part.seriesUUID, false, "777790", false, IsoVersion.v22)
        val converted = seriesRegistrationService.findById(part.seriesUUID).shouldNotBeNull()
        converted.mainProduct shouldBe false
        converted.isoCategory22 shouldBe "777790"
        converted.isoCategory shouldBe "999990"
    }

}