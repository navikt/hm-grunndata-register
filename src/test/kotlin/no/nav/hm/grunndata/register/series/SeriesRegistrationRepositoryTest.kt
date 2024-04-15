package no.nav.hm.grunndata.register.series

import io.kotest.common.runBlocking
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class SeriesRegistrationRepositoryTest(private val seriesRegistrationRepository: SeriesRegistrationRepository) {

    @Test
    fun crudTest() {
        val series = SeriesRegistration(
            id = UUID.randomUUID(),
            supplierId = UUID.randomUUID(),
            identifier = "HMDB-123",
            title = "Series 1",
            text = "Series 1 text",
            isoCategory = "12343212",
            status = SeriesStatus.ACTIVE,
            adminStatus = AdminStatus.PENDING,
            seriesData = SeriesDataDTO(media = setOf(
                MediaInfoDTO(uri = "http://example.com", type = MediaType.IMAGE, text = "image description", sourceUri = "http://example.com",  source = MediaSourceType.REGISTER)
            ))
        )
        runBlocking {
            val saved = seriesRegistrationRepository.save(series)
            val found = seriesRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            val updated = seriesRegistrationRepository.update(found.copy(title="Series 2",
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.APPROVED))
            updated.title shouldBe "Series 2"
            updated.status shouldBe SeriesStatus.ACTIVE
            updated.expired shouldBeAfter updated.created
            updated.draftStatus shouldBe DraftStatus.DONE
            updated.adminStatus shouldBe AdminStatus.APPROVED
            updated.isoCategory shouldBe "12343212"
            updated.seriesData.media.size shouldBe 1
            updated.seriesData.media.first().uri shouldBe "http://example.com"
        }
    }
}