package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.UUID

@Client("$CONTEXT_PATH/${SeriesController.API_V1_SERIES}")
interface SeriesControllerApiClient {
    @Get(uri = "/", consumes = [APPLICATION_JSON])
    fun findSeries(
        @CookieValue("JWT") jwt: String,
    ): Page<SeriesRegistrationDTO>

    @Get(uri = "?title=finnesikke", consumes = [APPLICATION_JSON])
    fun findSeriesWithTitle(
        @CookieValue("JWT") jwt: String,
    ): Page<SeriesRegistrationDTO>

    @Post(uri = "/", processes = [APPLICATION_JSON])
    fun createSeries(
        @CookieValue("JWT") jwt: String,
        @Body seriesRegistrationDTO: SeriesRegistrationDTO,
    ): SeriesRegistrationDTO

    @Put(uri = "/{id}", processes = [APPLICATION_JSON])
    fun updateSeries(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
        @Body seriesRegistrationDTO: SeriesRegistrationDTO,
    ): SeriesRegistrationDTO

    @Get(uri = "/{id}", consumes = [APPLICATION_JSON])
    fun readSeries(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    ): SeriesRegistrationDTO?
}
