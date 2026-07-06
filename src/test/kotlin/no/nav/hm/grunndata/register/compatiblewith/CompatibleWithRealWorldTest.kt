package no.nav.hm.grunndata.register.compatiblewith

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test

//@MicronautTest
class CompatibleWithRealWorldTest(private val compatibleAIFinder: CompatibleAIFinder) {

    //@Test
    fun findCompatibleProduct() {
        compatibleAIFinder.findCompatibleWhenNoMainProducts("Fotkontroll / helautomatisk betjening toaløfter Lift Easy", "Hepro As")
    }

}