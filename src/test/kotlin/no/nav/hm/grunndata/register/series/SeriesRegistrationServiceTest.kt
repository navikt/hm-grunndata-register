package no.nav.hm.grunndata.register.series

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.Authentication
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test

@MicronautTest
class SeriesRegistrationServiceTest(
    private val service: SeriesRegistrationService,
    private val repository: SeriesRegistrationRepository
) {
    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)


    @Test
    fun patchSeries() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()

        val originalSeries = SeriesRegistration(
            id = seriesId,
            supplierId = supplierId,
            identifier = "identifier",
            title = "title",
            text = "text",
            isoCategory = "12345678",
            draftStatus = DraftStatus.DRAFT,
            status = SeriesStatus.ACTIVE,
            adminStatus = AdminStatus.PENDING,
            seriesData = SeriesDataDTO(
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
                attributes = SeriesAttributesDTO(
                    keywords = listOf("keyword1", "keyword2"),
                    url = "url",
                )
            )
        )

        val originalSeriesDTO = originalSeries.toDTO()

        val patchUpdateDTO = UpdateSeriesRegistrationDTO(
            title = "new title",
            text = "new text",
            keywords = listOf("new keyword1", "new keyword2"),
            url = "new url",
        )

        val authentication = Authentication.build("marte", mapOf("supplierId" to supplierId.toString()))

        runBlocking {
            service.save(originalSeriesDTO)
            
            val patchedSeries = service.patchSeries(seriesId, patchUpdateDTO, authentication)

            patchedSeries.title shouldBe patchUpdateDTO.title
            patchedSeries.text shouldBe patchUpdateDTO.text
            patchedSeries.seriesData.attributes.keywords shouldBe patchUpdateDTO.keywords
            patchedSeries.seriesData.attributes.url shouldBe patchUpdateDTO.url

        }
    }
}