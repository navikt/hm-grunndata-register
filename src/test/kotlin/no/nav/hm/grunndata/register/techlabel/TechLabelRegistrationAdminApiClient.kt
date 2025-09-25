package no.nav.hm.grunndata.register.techlabel

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.*

@Client(id = "$CONTEXT_PATH/${TechLabelRegistrationAdminController.API_V1_ADMIN_TECHLABEL_REGISTRATIONS}")
interface TechLabelRegistrationAdminApiClient {

    @Get(uri = "/{id}", consumes = [MediaType.APPLICATION_JSON])
    fun getTechLabelById(@CookieValue("JWT") jwt: String, id: UUID): HttpResponse<TechLabelRegistrationDTO>

    @Post(uri= "/", consumes = [MediaType.APPLICATION_JSON])
    fun createTechLabel(@CookieValue("JWT") jwt: String, @Body dto: TechLabelCreateUpdateDTO): HttpResponse<TechLabelRegistrationDTO>

    @Put(uri = "/{id}", consumes = [MediaType.APPLICATION_JSON])
    fun updateTechLabel(@CookieValue("JWT") jwt: String, id: UUID, @Body dto: TechLabelCreateUpdateDTO): HttpResponse<TechLabelRegistrationDTO>
}