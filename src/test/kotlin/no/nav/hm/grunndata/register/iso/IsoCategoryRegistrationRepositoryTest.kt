package no.nav.hm.grunndata.register.iso

import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.register.REGISTER
import org.junit.jupiter.api.Test

@MicronautTest
class IsoCategoryRegistrationRepositoryTest(private val isoCategoryRepository: IsoCategoryRegistrationRepository) {

    @Test
    fun testCrudIsoCategory() {
        val testCategory = IsoCategoryRegistration(
            isoCode ="30300001",
            isoLevel = 4,
            isoTitle = "Hjelpemidler for røyking",
            isoTextShort = "Hjelpemidler for røyking",
            isoText = "Hjelpemidler som gjør det mulig for en person å røyke. Omfatter f.eks tilpassede askebegre, lightere og sigarettholdere. Smekker og forklær, se 09 03 39",
            isoTranslations = IsoTranslations(titleEn = "English title", textEn = "English text"),
            isActive = true,
            showTech = true,
            allowMulti = true,
            searchWords = listOf("Hjelpemidler", "røyking"),
            createdByUser = "tester",
            updatedByUser = "tester",
        )
        runBlocking {
            val saved = isoCategoryRepository.save(testCategory)
            saved.shouldNotBeNull()
            val read = isoCategoryRepository.findById("30300001")
            read.shouldNotBeNull()
            read.isoLevel shouldBe testCategory.isoLevel
            read.isoTitle shouldBe testCategory.isoTitle
            read.isoText shouldBe  testCategory.isoText
            read.isActive shouldBe testCategory.isActive
            read.showTech shouldBe testCategory.showTech
            read.allowMulti shouldBe testCategory.allowMulti
            read.isoTranslations.titleEn shouldBe  testCategory.isoTranslations.titleEn
            read.isoTranslations.textEn shouldBe testCategory.isoTranslations.textEn
            read.updated.shouldNotBeNull()
            read.created.shouldNotBeNull()
            read.searchWords.size shouldBe 2
            read.updatedBy shouldBe REGISTER
            read.createdBy shouldBe REGISTER
            read.updatedByUser shouldBe "tester"
            read.createdByUser shouldBe "tester"
        }
    }
}