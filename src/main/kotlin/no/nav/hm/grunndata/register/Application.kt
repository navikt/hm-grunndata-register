package no.nav.hm.grunndata.register

import io.micronaut.runtime.Micronaut

object Application {
    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build()
            .packages("no.nav.hm.grunndata.register")
            .mainClass(Application.javaClass)
            .start()
    }
}