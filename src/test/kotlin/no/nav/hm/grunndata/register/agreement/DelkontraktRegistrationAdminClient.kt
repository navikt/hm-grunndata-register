package no.nav.hm.grunndata.register.agreement

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationAdminController.Companion.API_V1_ADMIN_DELKONTRAKT_REGISTRATIONS
import java.util.*

@Client(id = "$CONTEXT_PATH/$API_V1_ADMIN_DELKONTRAKT_REGISTRATIONS")
interface DelkontraktRegistrationAdminClient {

    @Get(uri = "/{id}", consumes = [MediaType.APPLICATION_JSON])
    suspend fun getById(@CookieValue("JWT") jwt: String, id: UUID): HttpResponse<DelkontraktRegistrationDTO?>

    @Put(uri = "/{id}", consumes = [MediaType.APPLICATION_JSON])
    suspend fun updateDelkontrakt(
        @CookieValue("JWT") jwt: String,
        id: UUID,
        @Body dto: DelkontraktRegistrationDTO
    ): HttpResponse<DelkontraktRegistrationDTO>

    @Post(uri = "/", consumes = [MediaType.APPLICATION_JSON])
    suspend fun createDelkontrakt(
        @CookieValue("JWT") jwt: String,
        @Body dto: DelkontraktRegistrationDTO
    ): HttpResponse<DelkontraktRegistrationDTO>

    @Get(uri = "/agreement/{agreementId}", consumes = [MediaType.APPLICATION_JSON])
    suspend fun findByAgreementId(@CookieValue("JWT") jwt: String, agreementId: UUID):
            HttpResponse<List<DelkontraktRegistrationDTO>>
}