package no.nav.hm.grunndata.register.substitutt

import io.kotest.assertions.print.print
import org.junit.jupiter.api.Test
import substituttlister.ParseSubstituttliste
import java.io.IOException
import java.nio.file.Paths

class ParseSubstituttlisteTest {
    @Test
    @Throws(IOException::class)
    fun testReadExcel() {
        val filePath = Paths.get("src", "test", "resources","substituttlister", "personloftere.xlsx").toString()
        val result = ParseSubstituttliste().readExcel(filePath)

        result.print()



    }
}
