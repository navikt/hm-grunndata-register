package no.nav.hm.grunndata.register.iso

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH


@Client(id = "$CONTEXT_PATH/${IsoCategoryRegistrationAdminController.API_V1_ADMIN_ISOCATEGORY_REGISTRATIONS}")
interface IsoCategoryRegistrationAdminApiClient {

    @Get(uri = "/{isocode}", consumes = [MediaType.APPLICATION_JSON])
    fun getIsoCategoryByIsocode(@CookieValue("JWT") jwt: String, isocode: String): HttpResponse<IsoCategoryRegistrationDTO>

    @Post(uri = "/", consumes = [MediaType.APPLICATION_JSON])
    fun createIsoCategory(@CookieValue("JWT") jwt: String, dto: IsoCategoryRegistrationDTO): HttpResponse<IsoCategoryRegistrationDTO>

    @Put(uri = "/{isocode}", consumes = [MediaType.APPLICATION_JSON])
    fun updateIsoCategory(@CookieValue("JWT") jwt: String, isocode: String, dto: IsoCategoryRegistrationDTO): HttpResponse<IsoCategoryRegistrationDTO>

}