package no.nav.hm.grunndata.register

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = Info(
        title = "Hjelpemiddel Registrering API",
        version = "0.1",
        description = "API for Registreringsfrontend"
    )
)
object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
            .packages("no.nav.hm.grunndata.register")
            .mainClass(Application.javaClass)
            .start()
    }
}
