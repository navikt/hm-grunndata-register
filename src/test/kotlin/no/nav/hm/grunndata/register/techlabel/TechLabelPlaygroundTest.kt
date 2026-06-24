package no.nav.hm.grunndata.register.techlabel

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper


@MicronautTest
class TechLabelPlaygroundTest(val objectMapper: ObjectMapper) {

    @Test
    fun mappinTest() {
        val techlabels = objectMapper.readValue(TechLabelPlaygroundTest::class.java.getResource("/techlabels_normalized.json").openStream(),
            object : TypeReference<List<TechLabelMapping>>() {})

        val orignals = techlabels.map { it.original}
        val names = objectMapper.readValue(TechLabelPlaygroundTest::class.java.getResource("/techlabel_original_names.json").openStream(), Set::class.java) as Set<String>
        // check not found in database?
        val notFoundInNames = names.map { name -> if (!orignals.contains(name)) name else null }.filterNotNull()
        println("Navn som ikke er i listen: ${notFoundInNames.size}")
        notFoundInNames.forEach {
            println("${it}")
        }
        // check what will be normalized
        techlabels.forEach {
            if (it.original != it.normalized) println("normalized from ${it.original} to ${it.normalized}")
        }
    }
}
