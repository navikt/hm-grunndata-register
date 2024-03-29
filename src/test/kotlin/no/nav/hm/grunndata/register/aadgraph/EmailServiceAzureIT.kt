package no.nav.hm.grunndata.register.aadgraph

import com.microsoft.graph.models.BodyType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@MicronautTest
@Disabled
class EmailServiceAzureIT(private val emailServiceAzure: EmailServiceAzure) {

    @Test
    fun sendEmailAzure() {
        emailServiceAzure.sendSimpleMessage(
            "somemailadress",
            "Dette er en test",
            BodyType.TEXT,
            "Dette er en test",
        )
    }
}
