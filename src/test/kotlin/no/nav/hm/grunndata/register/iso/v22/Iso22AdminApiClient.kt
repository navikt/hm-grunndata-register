package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.rapid.dto.IsoCategory22DTO
import no.nav.hm.grunndata.register.CONTEXT_PATH

@Client("$CONTEXT_PATH")
interface Iso22AdminApiClient {

    @Post(uri = Iso22AdminController.ADMIN_API_V22_ISO, consumes = [MediaType.APPLICATION_JSON])
    fun createIso(@CookieValue("JWT") jwt: String, @Body iso: String): HttpResponse<Iso22DTO>

    @Get(uri = "${Iso22AdminController.ADMIN_API_V22_ISO}/{isocode}")
    fun getIso(@CookieValue("JWT") jwt: String, isocode: String): HttpResponse<Iso22DTO>

    @Put(uri = "${Iso22AdminController.ADMIN_API_V22_ISO}/{isocode}", consumes = [MediaType.APPLICATION_JSON])
    fun updateIso(@CookieValue("JWT") jwt: String, isocode: String, @Body iso: String): HttpResponse<Iso22DTO>

    @Get("/api/v22/isocategories")
    fun getAllCategories(): List<IsoCategory22DTO>

    @Get("/api/v22/isocategories/{isocode}")
    fun getCategory(isocode: String): IsoCategory22DTO?
}
