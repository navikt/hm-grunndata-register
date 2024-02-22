package no.nav.hm.grunndata.register.news

import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.NewsStatus
import no.nav.hm.grunndata.register.HMDB
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@MicronautTest
class NewsRegistrationServiceTest(private val newsRegistrationService: NewsRegistrationService) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun testExpirationPublish() {
        val expired = NewsRegistrationDTO(
            title = "Expired news", text = "This is an expired news",
            createdByUser = "tester", updatedByUser = "tester", expired = LocalDateTime.now().minusDays(1),
            createdBy = HMDB, updatedBy = HMDB, status = NewsStatus.ACTIVE
        )
        val published = NewsRegistrationDTO(
            title = "Published news",
            text = "This is a published news",
            createdByUser = "tester",
            updatedByUser = "tester",
            published = LocalDateTime.now(),
            expired = LocalDateTime.now().plusMonths(1),
            createdBy = HMDB,
            updatedBy = HMDB,
            status = NewsStatus.INACTIVE
        )
        runBlocking {
            val toBeExpired = newsRegistrationService.saveAndCreateEventIfNotDraft(expired, false)
            val toBePublished = newsRegistrationService.saveAndCreateEventIfNotDraft(published, false)
            toBeExpired.status shouldBe NewsStatus.INACTIVE
            toBePublished.status shouldBe NewsStatus.ACTIVE
        }
    }
}