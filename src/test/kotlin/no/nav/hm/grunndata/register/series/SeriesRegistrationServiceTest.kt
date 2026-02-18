package no.nav.hm.grunndata.register.series

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.Authentication
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test

@MicronautTest
class SeriesRegistrationServiceTest(
    private val service: SeriesRegistrationService,
    private val productRegistrationService: ProductRegistrationService
) {
    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)


    @Test
    fun patchSeries() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()

        val originalSeries = newSeries(seriesId, supplierId)

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
            service.save(originalSeries)

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
        val authentication = Authentication.build("marte", mapOf("supplierId" to supplierId.toString()))
        val originalSeries = newSeries(
            seriesId = seriesId,
            supplierId = supplierId,
            mediaUri = mediaUri
        )

        val newMediaPriority = listOf(MediaSort(uri = mediaUri, priority = 2))

        runBlocking {
            service.save(originalSeries)
            service.updateSeriesMediaPriority(seriesId, newMediaPriority, authentication)

            val seriesMedia = service.findById(seriesId)?.seriesData?.media
            seriesMedia.shouldNotBeNull()
            seriesMedia shouldHaveSize 3

            val media = seriesMedia.find { it.uri == mediaUri }
            media.shouldNotBeNull()
            media.priority shouldBe 2
        }
    }

    @Test
    fun `Delete media from series`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val mediaUri = "mediaUri"
        val authentication = Authentication.build("marte", mapOf("supplierId" to supplierId.toString()))
        val originalSeries = newSeries(
            seriesId = seriesId,
            supplierId = supplierId,
            mediaUri = mediaUri
        )

        val mediaToBeDeleted = listOf(mediaUri)

        runBlocking {
            service.save(originalSeries)
            service.deleteSeriesMedia(seriesId, mediaToBeDeleted, authentication)

            val seriesMedia = service.findById(seriesId)?.seriesData?.media
            seriesMedia.shouldNotBeNull()
            seriesMedia shouldHaveSize 2

            seriesMedia.find { it.uri == mediaUri } shouldBe null
        }
    }

    @Test
    fun `Add videos to series`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val mediaUri = "mediaUri"
        val authentication = Authentication.build("marte", mapOf("supplierId" to supplierId.toString()))
        val originalSeries = newSeries(
            seriesId = seriesId,
            supplierId = supplierId,
        )

        val videoTitle = "tittel"
        val videos = listOf(NewVideo(mediaUri, videoTitle))

        runBlocking {
            service.save(originalSeries)
            service.addVideos(seriesId, videos, authentication)

            val seriesMedia = service.findById(seriesId)?.seriesData?.media
            seriesMedia.shouldNotBeNull()
            seriesMedia shouldHaveSize 4

            val savedVideo = seriesMedia.find { it.uri == mediaUri }
            savedVideo.shouldNotBeNull()
            savedVideo.text shouldBe videoTitle
        }
    }

    @Test
    fun `Delete delete draft series and product variants`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val series = newSeries(seriesId, supplierId)


        val authentication = Authentication.build("marte", mapOf("supplierId" to supplierId.toString()))

        runBlocking {
            productRegistrationService.save(
                ProductRegistration(
                    id = UUID.randomUUID(),
                    supplierId = supplierId,
                    supplierRef = "123-abc",
                    hmsArtNr = "1234",
                    seriesUUID = seriesId,
                    articleName = "produkt 1",
                    productData = ProductData()
                )
            )

            service.save(series)

            service.deleteDraft(series, authentication)

            service.findById(seriesId) shouldBe null
            productRegistrationService.findAllBySeriesUuid(seriesId) shouldBe emptyList()
        }
    }

    @Test
    fun `Can't delete published series`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val series = SeriesRegistration(
            id = seriesId,
            supplierId = supplierId,
            identifier = "identifier",
            title = "ikke kladd",
            text = "dette er absolutt ikke en kladd",
            isoCategory = "1",
            seriesData = SeriesDataDTO(),
            published = LocalDateTime.now()
        )

        val authentication = Authentication.build("marte", mapOf("supplierId" to supplierId.toString()))

        runBlocking {
            productRegistrationService.save(
                ProductRegistration(
                    id = UUID.randomUUID(),
                    supplierId = supplierId,
                    supplierRef = "123-abc",
                    hmsArtNr = "1234",
                    seriesUUID = seriesId,
                    articleName = "produkt 1",
                    productData = ProductData()
                )
            )

            service.save(series)

            shouldThrow<BadRequestException> { service.deleteDraft(series, authentication) }

            service.findById(seriesId).shouldNotBeNull()
            productRegistrationService.findAllBySeriesUuid(seriesId).shouldNotBeEmpty()
        }
    }

    private fun newSeries(seriesId: UUID, supplierId: UUID, mediaUri: String = "uri") = SeriesRegistration(
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
}