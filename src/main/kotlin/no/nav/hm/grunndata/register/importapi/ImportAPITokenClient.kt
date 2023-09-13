package no.nav.hm.grunndata.register.importapi

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import java.util.*

@Client("\${grunndata.import.url}/internal/token")
interface ImportAPITokenClient {

    @Post("/{supplierId}", processes = [MediaType.APPLICATION_JSON])
    fun createSupplierToken(supplierId: UUID, @Header authorization: String): String

    @Post("/admin/{subject}", processes = [MediaType.APPLICATION_JSON])
    fun createAdminToken(subject: String, @Header authorization: String): String

}