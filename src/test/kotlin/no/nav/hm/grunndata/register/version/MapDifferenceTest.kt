package no.nav.hm.grunndata.register.version

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesAttributesDTO
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@MicronautTest
class MapDifferenceTest(private val objectMapper: ObjectMapper) {

    @Test
    fun testMapDifference() {
        val id = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val identifier = "identifier"
        val created = LocalDateTime.now()
        val expired = LocalDateTime.now()
        val updated = LocalDateTime.now()

        val seriesRegistrationDTO1 = SeriesRegistrationDTO(
            id = id,
            supplierId = supplierId,
            identifier = identifier,
            title = "title",
            text = "text",
            isoCategory = "12345678",
            created = created,
            expired = expired,
            updated = updated,
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
                        updated = updated
                    ),
                ),
                attributes = SeriesAttributesDTO(
                    keywords = listOf("keyword1", "keyword2"),
                    url = "http://example.com"
                )
            )
        )

        val seriesRegistrationDTO2 = SeriesRegistrationDTO(
            id = id,
            supplierId = supplierId,
            identifier = identifier,
            title = "title2",
            text = "text2",
            isoCategory = "12345679",
            created = created,
            expired = expired,
            updated = updated,
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
                        updated = updated,
                    )
                ),
                attributes = SeriesAttributesDTO(
                    keywords = listOf("keyword1", "keyword2"),
                    url = "http://example.com"
                )
            )
        )

        val map1: Map<String, Any> = objectMapper.convertValue(seriesRegistrationDTO1)
        val map2: Map<String, Any> = objectMapper.convertValue(seriesRegistrationDTO2)
        val difference = map1.difference(map2)
        difference.status shouldBe DiffStatus.DIFF
    }

    @Test
    fun `changes only in case should not be marked as change`() {
        val supplierId = UUID.randomUUID()
        val seriesId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val created = LocalDateTime.now()
        val expired = LocalDateTime.now()

        val prod1 = ProductRegistrationDTO(
            id = productId,
            created = created,
            expired = expired,
            seriesUUID = seriesId,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            articleName = "articleName",
            supplierId = supplierId,
            seriesId = seriesId.toString(),
            hmsArtNr = "hmsArtNr123",
            supplierRef = "supplierRef",
            productData = ProductData(
                techData = listOf(
                    TechData("size", "m", "string")
                ),
            )
        )

        val prod2 = ProductRegistrationDTO(
            id = productId,
            created = created,
            expired = expired,
            seriesUUID = seriesId,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            articleName = "articleName",
            supplierId = supplierId,
            seriesId = seriesId.toString(),
            hmsArtNr = "hmsArtNr123",
            supplierRef = "supplierRef",
            productData = ProductData(
                techData = listOf(
                    TechData("size", "M", "string")
                ),
            )
        )

        val prod3 = ProductRegistrationDTO(
            id = productId,
            created = created,
            expired = expired,
            seriesUUID = seriesId,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            articleName = "articleName",
            supplierId = supplierId,
            seriesId = seriesId.toString(),
            hmsArtNr = "hmsArtNr123",
            supplierRef = "supplierRef",
            productData = ProductData(
                techData = listOf(
                    TechData("size", "L", "string")
                ),
            )
        )

        val map1: Map<String, Any> = objectMapper.convertValue(prod1)
        val map2: Map<String, Any> = objectMapper.convertValue(prod2)
        val map3: Map<String, Any> = objectMapper.convertValue(prod3)

        val difference = map1.difference(map2)
        difference.status shouldBe DiffStatus.NO_DIFF

        val difference2 = map1.difference(map3)
        difference2.status shouldBe DiffStatus.DIFF


    }

}