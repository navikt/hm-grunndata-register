package no.nav.hm.grunndata.register.catalog

import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.register.error.BadRequestException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@MicronautTest
class ArticleTypeMappingTest {

    @Test
    fun `test mapArticleType`() {
        val type1 = mapArticleType("hj.middel", "ja")
        type1.mainProduct shouldBe true
        type1.accessory shouldBe true
        type1.sparePart shouldBe false

        val type2 = mapArticleType("hms del", "ja")
        type2.mainProduct shouldBe false
        type2.accessory shouldBe true
        type2.sparePart shouldBe false

        val type3 = mapArticleType("hms del", "nei")
        type3.mainProduct shouldBe false
        type3.accessory shouldBe false
        type3.sparePart shouldBe true

        val type4 = mapArticleType("hj.middel", "nei")
        type4.mainProduct shouldBe true
        type4.accessory shouldBe false
        type4.sparePart shouldBe false
        type4.service shouldBe false

        val type5 = mapArticleType("HMS Servicetjeneste", "nei")
        type5.mainProduct shouldBe false
        type5.accessory shouldBe false
        type5.sparePart shouldBe false
        type5.service shouldBe true

        assertThrows<BadRequestException> {
            mapArticleType("unknown type", "ja")
        }

    }
}