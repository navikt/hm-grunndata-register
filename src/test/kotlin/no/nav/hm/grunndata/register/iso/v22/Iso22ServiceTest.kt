package no.nav.hm.grunndata.register.iso.v22

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@MicronautTest
class Iso22ServiceTest(
    private val iso22Service: Iso22Service,
    private val iso22Repository: Iso22Repository,
) {

    private fun iso22(isoCode: String, isoTitle: String) =
        Iso22(isoCode = isoCode, isoTitle = isoTitle, createdByUser = "tester", updatedByUser = "tester")

    @Test
    fun `finner kategorier opprettet etter oppstart uten restart`() {
        runBlocking {
            iso22Repository.save(iso22("31310101", "Opprettet etter oppstart"))

            iso22Service.retrieveAll().map { it.isoCode } shouldContain "31310101"
            iso22Service.lookUpCode("31310101").shouldNotBeNull().isoTitle shouldBe "Opprettet etter oppstart"
        }
    }

    @Test
    fun `ukjent kode caches ikke, slik at den finnes etter senere opprettelse`() {
        runBlocking {
            iso22Service.lookUpCode("31310102").shouldBeNull()
            iso22Repository.save(iso22("31310102", "Opprettet senere"))
            iso22Service.lookUpCode("31310102").shouldNotBeNull()
        }
    }

    @Test
    fun `refresh plukker opp endringer gjort utenfor denne poden`() {
        runBlocking {
            val saved = iso22Repository.save(iso22("31310103", "Gammel tittel"))
            iso22Service.lookUpCode("31310103").shouldNotBeNull().isoTitle shouldBe "Gammel tittel"

            iso22Repository.update(saved.copy(isoTitle = "Ny tittel"))
            iso22Service.lookUpCode("31310103").shouldNotBeNull().isoTitle shouldBe "Gammel tittel"

            iso22Service.refresh()
            iso22Service.lookUpCode("31310103").shouldNotBeNull().isoTitle shouldBe "Ny tittel"
        }
    }
}
