package no.nav.hm.grunndata.register.series

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.cache.CacheManager
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
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.iso.IsoVersion
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.iso.v22.Iso22
import no.nav.hm.grunndata.register.iso.v22.Iso22Repository
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.DraftVariantDTO
import no.nav.hm.grunndata.register.techlabel.TechLabelRegistration
import no.nav.hm.grunndata.register.techlabel.TechLabelRegistrationRepository
import no.nav.hm.grunndata.register.techlabel.TechLabelType
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach

@MicronautTest
class SeriesRegistrationServiceTest(
    private val service: SeriesRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val iso22Repository: Iso22Repository,
    private val techLabelRepository: TechLabelRegistrationRepository,
    private val cacheManager: CacheManager<*>,
) {
    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun clearTechnicalLabelCache() {
        cacheManager.getCache("techlabels-all-labels22").invalidateAll()
    }

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
    fun `patchSeries saves isoCategory22`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val authentication = adminAuthentication()

        runBlocking {
            ensureIso22("33330101")
            service.save(newSeries(seriesId, supplierId))

            service.patchSeries(seriesId, UpdateSeriesRegistrationDTO(isoCategory22 = "33330101"), authentication)
            service.findById(seriesId).shouldNotBeNull().isoCategory22 shouldBe "33330101"
        }
    }

    @Test
    fun `default v16 patch honors changed ISO22 with unchanged ISO16 and uses ISO22 technical labels`(): Unit = runBlocking {
        ensureIso22("33330109")
        techLabelRepository.save(
            TechLabelRegistration(
                label = "ISO22 height", isoCode = "99999999", isoCode22 = "33330109",
                type = TechLabelType.N, unit = "cm", sort = 0, definition = null,
            )
        )
        listOf(false, true).forEach { reset ->
            val series = newSeries(UUID.randomUUID(), UUID.randomUUID()).copy(isoCategory22 = "33330108")
            service.save(series)
            val product = productRegistrationService.save(
                ProductRegistration(
                    id = UUID.randomUUID(), seriesUUID = series.id, supplierId = series.supplierId,
                    supplierRef = UUID.randomUUID().toString(), articleName = "Variant", hmsArtNr = null,
                    productData = ProductData(techData = listOf(
                        TechData(key = "ISO22 height", value = "42", unit = "cm"),
                        TechData(key = "Old field", value = "10", unit = "kg"),
                    )),
                )
            )

            service.patchSeries(
                series.id,
                UpdateSeriesRegistrationDTO(
                    isoCategory = series.isoCategory, isoCategory22 = "33330109", resetTechnicalData = reset,
                ),
                supplierAuthentication(series.supplierId),
            )

            val updated = service.findById(series.id).shouldNotBeNull()
            updated.isoCategory22 shouldBe "33330109"
            updated.isoCategory shouldBe series.isoCategory
            productRegistrationService.findById(product.id).shouldNotBeNull().productData.techData shouldBe
                listOf(TechData(key = "ISO22 height", value = if (reset) "" else "42", unit = "cm"))
        }
    }

    @Test
    fun `patchSeries normalizes isoCategory22 before validating it`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val authentication = adminAuthentication()

        runBlocking {
            ensureIso22("33330102")
            service.save(newSeries(seriesId, supplierId))

            service.patchSeries(seriesId, UpdateSeriesRegistrationDTO(isoCategory22 = " 33 33 01 02 "), authentication)
            service.findById(seriesId).shouldNotBeNull().isoCategory22 shouldBe "33330102"
        }
    }

    @Test
    fun `patchSeries avviser ukjent isoCategory22 og beholder lagret verdi`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val authentication = adminAuthentication()

        runBlocking {
            ensureIso22("33330103")
            service.save(newSeries(seriesId, supplierId).copy(isoCategory22 = "33330103"))

            shouldThrow<BadRequestException> {
                service.patchSeries(seriesId, UpdateSeriesRegistrationDTO(isoCategory22 = "33339999"), authentication)
            }
            service.findById(seriesId).shouldNotBeNull().isoCategory22 shouldBe "33330103"
        }
    }

    @Test
    fun `supplier and HMS can change isoCategory22 under existing access rules`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()

        runBlocking {
            ensureIso22("33330104")
            ensureIso22("33330105")
            service.save(newSeries(seriesId, supplierId).copy(isoCategory22 = "33330104"))

            service.patchSeries(
                seriesId,
                UpdateSeriesRegistrationDTO(isoCategory22 = "33330105"),
                supplierAuthentication(supplierId),
            )
            service.findById(seriesId).shouldNotBeNull().isoCategory22 shouldBe "33330105"
            shouldThrow<BadRequestException> {
                service.patchSeries(
                    seriesId,
                    UpdateSeriesRegistrationDTO(isoCategory22 = "33330104"),
                    Authentication.build("hms", listOf(Roles.ROLE_HMS), emptyMap()),
                )
            }
            service.update(service.findById(seriesId).shouldNotBeNull().copy(mainProduct = false))
            service.patchSeries(
                seriesId,
                UpdateSeriesRegistrationDTO(isoCategory22 = "33330104"),
                Authentication.build("hms", listOf(Roles.ROLE_HMS), emptyMap()),
            )
            service.findById(seriesId).shouldNotBeNull().isoCategory22 shouldBe "33330104"
        }
    }

    @Test
    fun `supplier cannot change another supplier ISO22 category`(): Unit = runBlocking {
        val series = newSeries(UUID.randomUUID(), UUID.randomUUID())
        ensureIso22("33330105")
        service.save(series)
        shouldThrow<BadRequestException> {
            service.patchSeries(
                series.id,
                UpdateSeriesRegistrationDTO(isoCategory22 = "33330105"),
                supplierAuthentication(UUID.randomUUID()),
            )
        }
        service.findById(series.id).shouldNotBeNull().isoCategory22 shouldBe null
    }

    @Test
    fun `leverandør kan sende uendret isoCategory22 sammen med andre endringer`() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()

        runBlocking {
            ensureIso22("33330106")
            service.save(newSeries(seriesId, supplierId).copy(isoCategory22 = "33330106"))
            val technicalData = listOf(
                TechData(key = "ISO16-only height", value = "42", unit = "cm"),
                TechData(key = "ISO16-only weight", value = "10", unit = "kg"),
            )
            val product = productRegistrationService.save(
                ProductRegistration(
                    id = UUID.randomUUID(), seriesUUID = seriesId, supplierId = supplierId,
                    supplierRef = UUID.randomUUID().toString(), articleName = "Variant", hmsArtNr = null,
                    productData = ProductData(techData = technicalData),
                )
            )

            listOf(null, false).forEach { reset ->
                service.patchSeries(
                    seriesId,
                    UpdateSeriesRegistrationDTO(
                        title = "leverandørtittel", isoCategory22 = " 33 33 01 06 ", resetTechnicalData = reset,
                    ),
                    supplierAuthentication(supplierId),
                )
                productRegistrationService.findById(product.id).shouldNotBeNull().productData.techData shouldBe technicalData
            }

            val patched = service.findById(seriesId).shouldNotBeNull()
            patched.title shouldBe "leverandørtittel"
            patched.isoCategory22 shouldBe "33330106"
        }
    }

    private fun adminAuthentication() = Authentication.build("admin", listOf(Roles.ROLE_ADMIN), emptyMap())

    @Test
    fun `ISO22 category changes regenerate and reset active technical fields and initialize variants`() = runBlocking {
        val series = newSeries(UUID.randomUUID(), UUID.randomUUID())
        ensureIso22("33330107")
        service.save(series)
        listOf("Shared", "New").forEach { label ->
            techLabelRepository.save(
                TechLabelRegistration(
                    label = label, isoCode = "99999999", isoCode22 = "33330107",
                    type = TechLabelType.N, unit = "cm", sort = 0, definition = null,
                )
            )
        }
        val product = productRegistrationService.save(
            ProductRegistration(
                id = UUID.randomUUID(), seriesUUID = series.id, supplierId = series.supplierId,
                supplierRef = UUID.randomUUID().toString(), articleName = "Variant", hmsArtNr = null,
                productData = ProductData(techData = listOf(
                    TechData(key = "Shared", value = "42", unit = "cm"),
                    TechData(key = "Old", value = "10", unit = "kg"),
                )),
            )
        )
        service.patchSeries(
            series.id, UpdateSeriesRegistrationDTO(isoCategory22 = "33330107", resetTechnicalData = false),
            supplierAuthentication(series.supplierId), IsoVersion.v22,
        )
        val retained = productRegistrationService.findById(product.id).shouldNotBeNull().productData.techData
        retained.map { it.key }.toSet() shouldBe setOf("Shared", "New")
        retained.find { it.key == "Shared" }!!.value shouldBe "42"
        retained.find { it.key == "New" }!!.value shouldBe ""
        productRegistrationService.findByStartsWithIsoCategoryAndTechLabelKeyUnit(
            "333301", key = "Shared", unit = "cm", isoVersion = IsoVersion.v22,
        ).map { it.id } shouldBe listOf(product.id)
        productRegistrationService.findByStartsWithIsoCategoryAndTechLabelKeyUnit(
            "333301", key = "Shared", unit = "cm",
        ).isEmpty() shouldBe true
        service.findById(series.id).shouldNotBeNull().isoCategory shouldBe series.isoCategory
        val variant = productRegistrationService.createDraft(
            series.id, DraftVariantDTO("Second", UUID.randomUUID().toString()),
            supplierAuthentication(series.supplierId), IsoVersion.v22,
        )
        variant.productData.techData.map { it.key }.toSet() shouldBe setOf("Shared", "New")
        variant.productData.techData.find { it.key == "Shared" }!!.value shouldBe "42"
        service.patchSeries(
            series.id, UpdateSeriesRegistrationDTO(isoCategory22 = "33330107", resetTechnicalData = true),
            supplierAuthentication(series.supplierId), IsoVersion.v22,
        )
        productRegistrationService.findAllBySeriesUuid(series.id).forEach { updated ->
            updated.productData.techData.all { it.value.isEmpty() } shouldBe true
        }
    }

    private fun supplierAuthentication(supplierId: UUID) =
        Authentication.build("leverandør", listOf(Roles.ROLE_SUPPLIER), mapOf("supplierId" to supplierId.toString()))

    private suspend fun ensureIso22(isoCode: String) {
        if (iso22Repository.findByIsoCode(isoCode) == null) {
            iso22Repository.save(
                Iso22(isoCode = isoCode, isoTitle = "Test $isoCode", createdByUser = "tester", updatedByUser = "tester")
            )
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
            service.save(series)
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
            service.save(series)
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