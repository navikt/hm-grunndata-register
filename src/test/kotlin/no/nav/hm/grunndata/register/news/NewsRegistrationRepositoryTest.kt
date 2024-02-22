package no.nav.hm.grunndata.register.news

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.NewsStatus
import no.nav.hm.grunndata.register.HMDB
import org.junit.jupiter.api.Test

@MicronautTest
class NewsRegistrationRepositoryTest(private val newsRegistrationRepository: NewsRegistrationRepository) {

    @Test
    fun crudTest() {
        val newsRegistration = NewsRegistration(title = "Test news", text = "This is a test news", createdByUser = "tester",
            updatedByUser = "tester", createdBy = HMDB, updatedBy = HMDB)
        runBlocking {
            val saved = newsRegistrationRepository.save(newsRegistration)
            val found = newsRegistrationRepository.findById(saved.id)
            found.shouldNotBeNull()
            found.status shouldBe NewsStatus.ACTIVE
            found.title shouldBe "Test news"
            found.text shouldBe "This is a test news"
            found.createdByUser shouldBe "tester"
            found.createdBy shouldBe HMDB
            found.author shouldBe "Admin"
            val updated = newsRegistrationRepository.update(found.copy(text = "This is a test news updated", status = NewsStatus.INACTIVE, updatedByUser = "tester"))
            updated.shouldNotBeNull()
            updated.text shouldBe "This is a test news updated"
            updated.status shouldBe NewsStatus.INACTIVE


        }

    }
}