package no.nav.hm.grunndata.register.aadgraph

import com.microsoft.graph.models.BodyType
import io.micronaut.context.annotation.Requires
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test

@MicronautTest
@Requires(env = ["ignore-me"])
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
