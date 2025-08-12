package no.nav.hm.grunndata.register.part

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.Authentication
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.product.ProductRegistrationService
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

            partService.changeToMainProduct(part.seriesUUID)
            val seriesAsMainProduct = seriesRegistrationService.findById(part.seriesUUID)

            seriesAsMainProduct?.mainProduct shouldBe true


        }
    }


}