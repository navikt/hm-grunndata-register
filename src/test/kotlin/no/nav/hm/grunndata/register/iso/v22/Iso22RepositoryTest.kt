package no.nav.hm.grunndata.register.iso.v22

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.iso.IsoTranslations
import org.junit.jupiter.api.Test

@MicronautTest
class Iso22RepositoryTest(private val repository: Iso22Repository) {

    @Test
    fun testCrudRepository() {
        val iso22 = Iso22(
            isoCode = "30300001",
            isoTitle = "Hjelpemidler for røyking",
            isoTitleShort = "Hjelpemidler for røyking",
            isoText = "Hjelpemidler som gjør det mulig for en person å røyke.",
            isoTranslations = IsoTranslations(titleEn = "English title", textEn = "English text"),
            searchWords = listOf("Hjelpemidler", "røyking"),
            createdByUser = "tester",
            updatedByUser = "tester",
        )
        runBlocking {
            val saved = repository.save(iso22)
            saved.shouldNotBeNull()

            val read = repository.findById(saved.id)
            read.shouldNotBeNull()
            read!!
            read.isoCode shouldBe iso22.isoCode
            read.isoTitle shouldBe iso22.isoTitle
            read.isoTitleShort shouldBe iso22.isoTitleShort
            read.isoText shouldBe iso22.isoText
            read.isoTranslations.titleEn shouldBe iso22.isoTranslations.titleEn
            read.isoTranslations.textEn shouldBe iso22.isoTranslations.textEn
            read.searchWords.size shouldBe 2
            read.updatedBy shouldBe REGISTER
            read.createdBy shouldBe REGISTER
            read.updatedByUser shouldBe "tester"
            read.createdByUser shouldBe "tester"
            read.created.shouldNotBeNull()
            read.updated.shouldNotBeNull()
        }
    }
}