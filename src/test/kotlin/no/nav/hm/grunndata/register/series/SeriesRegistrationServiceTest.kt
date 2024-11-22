package no.nav.hm.grunndata.register.series

import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
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

        val patchUpdateDTO2 = UpdateSeriesRegistrationDTO(
            title = "newer title",
            text = null,
            keywords = listOf("newer keyword1", "newer keyword2"),
            url = "newer url",
        )

        val authentication = Authentication.build("marte", mapOf("supplierId" to supplierId.toString()))

        runBlocking {
            service.save(originalSeriesDTO)

            var patchedSeries = service.patchSeries(seriesId, patchUpdateDTO, authentication)

            // same as patchDTO
            patchedSeries.title shouldBe patchUpdateDTO.title
            patchedSeries.text shouldBe patchUpdateDTO.text
            patchedSeries.seriesData.attributes.keywords shouldBe patchUpdateDTO.keywords
            patchedSeries.seriesData.attributes.url shouldBe patchUpdateDTO.url

            // text should be old value
            patchedSeries = service.patchSeries(seriesId, patchUpdateDTO2, authentication)

            patchedSeries.title shouldBe patchUpdateDTO2.title
            patchedSeries.text shouldBe patchUpdateDTO.text
            patchedSeries.seriesData.attributes.keywords shouldBe patchUpdateDTO2.keywords
            patchedSeries.seriesData.attributes.url shouldBe patchUpdateDTO2.url

        }
    }

    @Test
    fun `Change media priority`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val mediaUri = "mediaUri"

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
                        uri = mediaUri,
                        priority = 1,
                        type = MediaType.IMAGE,
                        text = "text",
                        source = MediaSourceType.REGISTER,
                        updated = LocalDateTime.now(),
                    ),
                    MediaInfoDTO(
                        sourceUri = "sourceUri2",
                        filename = "filename2",
                        uri = "uri2",
                        priority = 2,
                        type = MediaType.IMAGE,
                        text = "text2",
                        source = MediaSourceType.REGISTER,
                        updated = LocalDateTime.now(),
                    ),
                    MediaInfoDTO(
                        sourceUri = "sourceUri3",
                        filename = "filename2",
                        uri = "uri3",
                        priority = 3,
                        type = MediaType.IMAGE,
                        text = "text3",
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

        val authentication = Authentication.build("marte", mapOf("supplierId" to supplierId.toString()))

        val newMediaPriority = listOf(MediaSort(uri = mediaUri, priority = 2))

        runBlocking {
            service.save(originalSeries)

            service.updateSeriesMediaPriority(originalSeries, newMediaPriority, authentication)

            val series = service.findById(seriesId)
            series.shouldNotBeNull()
            val seriesMedia = series.seriesData.media
            seriesMedia.shouldNotBeNull()
            seriesMedia shouldHaveSize 1

            val media = seriesMedia.first()
            media.uri shouldBe mediaUri
            media.priority shouldBe 2


            println(seriesMedia)
        }

    }
}