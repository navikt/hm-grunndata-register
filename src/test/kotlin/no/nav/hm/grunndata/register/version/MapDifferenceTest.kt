package no.nav.hm.grunndata.register.version

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.series.SeriesAttributesDTO
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import org.junit.jupiter.api.Test

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

}