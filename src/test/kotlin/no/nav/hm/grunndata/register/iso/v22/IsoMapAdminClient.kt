package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.UUID

@Client(id = "$CONTEXT_PATH/${IsoMapAdminController.API_V1_ADMIN_ISOMAP}")
interface IsoMapAdminApiClient {

    @Get("/")
    fun getAllIsoMaps(@CookieValue("JWT") jwt: String): List<IsoMapDTO>

    @Post(uri = "/", consumes = [MediaType.APPLICATION_JSON])
    fun createIsoMap(@CookieValue("JWT") jwt: String, isoMap: IsoMapDTO): HttpResponse<IsoMapDTO>

    @Put(uri = "/{id}", consumes = [MediaType.APPLICATION_JSON])
    fun updateIsoMap(@CookieValue("JWT") jwt: String, id: UUID, isoMap: IsoMapDTO): HttpResponse<IsoMapDTO>
}