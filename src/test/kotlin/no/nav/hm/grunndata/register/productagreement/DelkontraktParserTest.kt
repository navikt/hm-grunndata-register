package no.nav.hm.grunndata.register.productagreement

import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test

@MicronautTest
class DelkontraktParserTest {

    @Test
    fun testDelkontraktNrExtract() {
        val del1 = "d1r1"
        val del2 = "d1Ar1"
        val del3 = "d1Br99" // mean no rank
        val del4 = "d1r"   // mean no rank
        val del5 = "d14"   // mean no rank
        val del6 = "d21d22"

        parsedelkontraktNr(del1) shouldBe listOf(Pair("1", 1))
        parsedelkontraktNr(del2) shouldBe listOf(Pair("1A", 1))
        parsedelkontraktNr(del3) shouldBe listOf(Pair("1B", 99))
        parsedelkontraktNr(del4) shouldBe listOf(Pair("1", 99))
        parsedelkontraktNr(del5) shouldBe listOf(Pair("14", 99))
        parsedelkontraktNr(del6) shouldBe listOf(Pair("21", 99), Pair("22", 99))

    }
}