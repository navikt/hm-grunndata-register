package no.nav.hm.grunndata.register.series

import io.kotest.matchers.nulls.shouldNotBeNull
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.MediaType
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import org.junit.jupiter.api.Test

@MicronautTest
class SeriesRegistrationVersionRepositoryTest(private val seriesRegistrationVersionRepository: SeriesRegistrationVersionRepository) {

    @Test
    fun crudTest() {
        val seriesId = UUID.randomUUID()
        val seriesDTO = SeriesRegistrationDTO(
            id = seriesId,
            supplierId = UUID.randomUUID(),
            identifier = "identifier",
            title = "title",
            text = "text",
            isoCategory = "12345678",
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
            ),
            draftStatus = DraftStatus.DONE,
            status = SeriesStatus.ACTIVE,
            adminStatus = AdminStatus.APPROVED,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now(),
            expired = LocalDateTime.now().plusYears(15),
            published = LocalDateTime.now(),
            createdBy = "createdBy",
            updatedBy = "updatedBy",
            updatedByUser = "updatedByUser",
            createdByUser = "createdByUser",
            createdByAdmin = false,
            count = 0,
            countDrafts = 0,
            countPublished = 0,
            countPending = 0,
            countDeclined = 0,
            version = 1
        )
        val seriesRegistrationVersion = SeriesRegistrationVersion(
            versionId = UUID.randomUUID(),
            seriesId = seriesDTO.id,
            status = seriesDTO.status,
            adminStatus = seriesDTO.adminStatus,
            draftStatus = seriesDTO.draftStatus,
            seriesRegistration = seriesDTO,
            version = seriesDTO.version,
            updated = seriesDTO.updated
        )
        runBlocking {
            val saved = seriesRegistrationVersionRepository.save(seriesRegistrationVersion)
            saved.shouldNotBeNull()
            val found = seriesRegistrationVersionRepository.findById(seriesRegistrationVersion.versionId)
            found.shouldNotBeNull()
            val approved = seriesRegistrationVersionRepository.findOneByDraftStatusAndAdminStatusOrderByVersionDesc(DraftStatus.DONE, AdminStatus.APPROVED)
            approved.shouldNotBeNull()

        }

    }
}