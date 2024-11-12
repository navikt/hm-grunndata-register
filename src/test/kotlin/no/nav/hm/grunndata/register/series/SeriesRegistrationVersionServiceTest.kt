package no.nav.hm.grunndata.register.series

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
import no.nav.hm.grunndata.register.series.version.SeriesRegistrationVersion
import no.nav.hm.grunndata.register.series.version.SeriesRegistrationVersionService
import no.nav.hm.grunndata.register.version.DiffStatus
import no.nav.hm.grunndata.register.version.Difference
import org.junit.jupiter.api.Test

@MicronautTest
class SeriesRegistrationVersionServiceTest(private val seriesRegistrationVersionService: SeriesRegistrationVersionService, private val seriesRegistrationRepository: SeriesRegistrationRepository ) {

    @Test
    fun testFindLastApprovedVersion() {
        val seriesId = UUID.randomUUID()
        val supplierId = UUID.randomUUID()
        val pending = SeriesRegistration(
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
                    url = "url"
                )
            )
        )

        runBlocking {
            val saved = seriesRegistrationRepository.save(pending)
            saved.shouldNotBeNull()
            val approved = seriesRegistrationRepository.update(saved.copy(
                draftStatus = DraftStatus.DONE,
                adminStatus = AdminStatus.APPROVED,
                title = "title2",
                seriesData = SeriesDataDTO(
                    media = setOf(
                        MediaInfoDTO(
                            sourceUri = "sourceUri2",
                            filename = "filename2",
                            uri = "uri2",
                            priority = 1,
                            type = MediaType.IMAGE,
                            text = "text2",
                            source = MediaSourceType.REGISTER,
                            updated = LocalDateTime.now(),
                        )
                    ),
                    attributes = SeriesAttributesDTO(
                        keywords = listOf("keyword1", "keyword2", "keyword3"),
                        url = "url2"
                    )
                )
            ))
            approved.shouldNotBeNull()
            val difference: Difference<String, Any> = seriesRegistrationVersionService.diffVersions(saved, approved)
            difference.status shouldBe DiffStatus.DIFF
            val pending = seriesRegistrationRepository.update(
                approved.copy(
                    draftStatus = DraftStatus.DONE,
                    adminStatus = AdminStatus.PENDING,
                    title = "Venter på godkjenning",
                    seriesData = SeriesDataDTO(
                        media = setOf(
                            MediaInfoDTO(
                                sourceUri = "sourceUri2",
                                filename = "filename2",
                                uri = "uri2",
                                priority = 1,
                                type = MediaType.IMAGE,
                                text = "text2",
                                source = MediaSourceType.REGISTER,
                                updated = LocalDateTime.now(),
                            )
                        ),
                        attributes = SeriesAttributesDTO(
                            keywords = listOf("keyword1", "keyword2", "keyword3"),
                            url = "url2"
                        )
                    )
                )
            )
            val pendingVersion = seriesRegistrationVersionService.save(
                SeriesRegistrationVersion(
                seriesId = pending.id,
                status = pending.status,
                adminStatus = pending.adminStatus,
                draftStatus = pending.draftStatus,
                seriesRegistration = pending,
                updatedBy = pending.updatedBy,
                updated = pending.updated
                )
            )
            val diffSinceLastApproved: Difference<String, Any> = seriesRegistrationVersionService.diffWithLastApprovedVersion(pendingVersion)
            diffSinceLastApproved.status shouldBe DiffStatus.DIFF
            diffSinceLastApproved.diff.entriesDiffering.size shouldBe 2
            diffSinceLastApproved.diff.entriesOnlyOnLeft.size shouldBe 0
            println(diffSinceLastApproved)

        }

    }
}