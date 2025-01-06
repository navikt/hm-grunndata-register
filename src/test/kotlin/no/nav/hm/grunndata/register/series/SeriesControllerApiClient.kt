package no.nav.hm.grunndata.register.series

import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.UUID

@Client("$CONTEXT_PATH/${SeriesRegistrationController.API_V1_SERIES}")
interface SeriesControllerApiClient {
    @Post(uri = "/draftWith", processes = [APPLICATION_JSON])
    fun createDraft(
        @CookieValue("JWT") jwt: String,
        @Body seriesRegistrationDTO: SeriesDraftWithDTO,
    ): SeriesRegistrationDTO

    @Put(uri = "/request-approval/{id}")
    fun setSeriesToPendingApproval(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    )
}
