package no.nav.hm.grunndata.register.productagreement

import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.register.catalog.parseHMSNr
import no.nav.hm.grunndata.register.catalog.parsedelkontraktNr
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
        val del7 = "d21, d22, d23"
        val del8 = "d8r2"
        parsedelkontraktNr(del1) shouldBe listOf(Pair("1", 1))
        parsedelkontraktNr(del2) shouldBe listOf(Pair("1A", 1))
        parsedelkontraktNr(del3) shouldBe listOf(Pair("1B", 99))
        parsedelkontraktNr(del4) shouldBe listOf(Pair("1", 99))
        parsedelkontraktNr(del5) shouldBe listOf(Pair("14", 99))
        parsedelkontraktNr(del6) shouldBe listOf(Pair("21", 99), Pair("22", 99))
        parsedelkontraktNr(del7) shouldBe listOf(Pair("21", 99), Pair("22", 99), Pair("23", 99))
        parsedelkontraktNr(del8) shouldBe listOf(Pair("8", 2))

    }

    @Test
    fun testParseHMSNR() {
        val nr1 = "123.0"
        val nr2 = "12345"
        val nr3 = "066666"
        val nr4 = "123456"
        val nr5 = "006019"

        parseHMSNr(nr1) shouldBe "000123"
        parseHMSNr(nr2) shouldBe "012345"
        parseHMSNr(nr3) shouldBe "066666"
        parseHMSNr(nr4) shouldBe "123456"
        parseHMSNr(nr5) shouldBe "006019"
    }
}