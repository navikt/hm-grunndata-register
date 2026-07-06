package no.nav.hm.grunndata.register.compatiblewith

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@MicronautTest
//@Disabled("Only for real world")
class CompatibleWithRealWorldTest(private val compatibleAIFinder: CompatibleAIFinder) {

    @Test
    fun findCompatibleProduct() {
        compatibleAIFinder.findCompatibleWhenNoMainProducts("Fotkontroll / helautomatisk betjening toaløfter Lift Easy", "Hepro As")
    }

}