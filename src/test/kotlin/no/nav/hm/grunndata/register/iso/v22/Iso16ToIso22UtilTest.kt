package no.nav.hm.grunndata.register.iso.v22

import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.iso.IsoCategoryRegistration
import no.nav.hm.grunndata.register.iso.IsoCategoryRegistrationRepository
import org.junit.jupiter.api.Test

@MicronautTest
class Iso16ToIso22UtilTest {
    @Test
    fun `createOrUpdateIso16To22Map saves SAME mapping when iso16 and iso22 are identical`() = runBlocking {
        val isoCategoryRepository = mockk<IsoCategoryRegistrationRepository>(relaxed = true)
        val isoMapRepository = mockk<IsoMapRepository>(relaxed = true)
        val iso22Repository = mockk<Iso22Repository>(relaxed = true)
        val isoMapper = mockk<IsoMapper>(relaxed = true)

        val util = Iso16ToIso22Util(isoCategoryRepository, isoMapRepository, iso22Repository, isoMapper)

        val iso16Nat = IsoCategoryRegistration(
            isoCode = "12345678",
            isoTitle = "Test title",
            isoText = "Test text",
            isoTextShort = "Test text short",
            createdByUser = "system",
            updatedByUser = "system",
            isoLevel = 4
        )

        val mapped = IsoMap(
            code16 = "12345678",
            code22 = "12345678",
            mapEnum = listOf(IsoMapEnum.SAME),
            verified = true
        )

        coEvery { isoMapper.mapIso16To22("12345678") } returns mapped
        coEvery { isoMapRepository.findByCode16AndCode22("12345678", "12345678") } returns null
        coEvery { isoMapRepository.save(any()) } returnsArgument 0

        val result = util.createOrUpdateIso16To22Map(iso16Nat)

        result?.code16 shouldBe "12345678"
        result?.code22 shouldBe "12345678"
        result?.verified shouldBe true

        coVerify(exactly = 1) {
            isoMapRepository.save(
                match {
                    it.code16 == "12345678" &&
                            it.code22 == "12345678" &&
                            it.mapEnum == listOf(IsoMapEnum.SAME) &&
                            it.verified
                }
            )
        }
    }

    @Test
    fun `createOrUpdateIso16To22Map stores mapped code when code has changed`() = runBlocking {
        val isoCategoryRepository = mockk<IsoCategoryRegistrationRepository>(relaxed = true)
        val isoMapRepository = mockk<IsoMapRepository>(relaxed = true)
        val iso22Repository = mockk<Iso22Repository>(relaxed = true)
        val isoMapper = mockk<IsoMapper>(relaxed = true)

        val util = Iso16ToIso22Util(isoCategoryRepository, isoMapRepository, iso22Repository, isoMapper)

        val iso16Nat = IsoCategoryRegistration(
            isoCode = "12345678",
            isoTitle = "Test title",
            isoText = "Test text",
            isoTextShort = "Test text short",
            createdByUser = "system",
            updatedByUser = "system",
            isoLevel = 4
        )

        val mapped = IsoMap(
            code16 = "12345678",
            code22 = "22000000",
            mapEnum = listOf(IsoMapEnum.CHANGED_CODE_SAME_HEADER),
            verified = false
        )

        coEvery { isoMapper.mapIso16To22("12345678") } returns mapped
        coEvery { isoMapRepository.findByCode16AndCode22("12345678", "22000000") } returns null
        coEvery { isoMapRepository.save(any()) } returnsArgument 0

        val result = util.createOrUpdateIso16To22Map(iso16Nat)

        result?.code16 shouldBe "12345678"
        result?.code22 shouldBe "22000000"
        result?.verified shouldBe false

        coVerify(exactly = 1) {
            isoMapRepository.save(
                match {
                    it.code16 == "12345678" &&
                            it.code22 == "22000000" &&
                            it.mapEnum == listOf(IsoMapEnum.CHANGED_CODE_SAME_HEADER) &&
                            !it.verified
                }
            )
        }
    }

}