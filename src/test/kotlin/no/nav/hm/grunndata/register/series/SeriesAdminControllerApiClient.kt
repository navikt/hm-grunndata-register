package no.nav.hm.grunndata.register.series

import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.UUID

@Client("$CONTEXT_PATH/${SeriesRegistrationAdminController.API_V1_SERIES}")
interface SeriesAdminControllerApiClient {

    @Put(uri = "/approve/{id}", processes = [APPLICATION_JSON])
    fun approveSeries(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    ): SeriesRegistrationDTO


}
