package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Patch
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.UUID

@Client("$CONTEXT_PATH/${SeriesRegistrationCommonController.API_V1_SERIES}")
interface SeriesCommonControllerApiClient {

    @Post(uri = "/supplier/{supplierId}/draftWith", processes = [APPLICATION_JSON])
    fun createDraft(
        @CookieValue("JWT") jwt: String,
        @PathVariable supplierId: UUID,
        @Body seriesRegistrationDTO: SeriesDraftWithDTO,
    ): SeriesDraftResponse

    @Patch(uri = "/{id}", processes = [APPLICATION_JSON])
    fun updateSeries(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
        @Body updateSeriesRegistrationDTO: UpdateSeriesRegistrationDTO,
    )

    @Get(uri = "/{id}", consumes = [APPLICATION_JSON])
    fun readSeries(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    ): SeriesRegistrationDTOV2?

    @Get(uri = "/?title={title}", processes = [APPLICATION_JSON])
    fun findSeriesByTitle(
        @CookieValue("JWT") jwt: String,
        title: String,
    ): Page<SeriesSearchDTO>

    @Get(uri = "/variant-id/{variantIdentifier}", consumes = [APPLICATION_JSON])
    fun findSeriesByVariantIdentifier(
        @CookieValue("JWT") jwt: String,
        @PathVariable variantIdentifier: String,
    ): SeriesSearchDTO?

    @Put(uri = "/series-to-inactive/{id}")
    fun setSeriesToInactive(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    )

    @Put(uri = "/series-to-active/{id}")
    fun setSeriesToActive(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    )

    @Put(uri = "/series_to-draft/{id}")
    fun setSeriesToDraft(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    )

    @Delete(uri = "/{id}")
    fun setSeriesToDeleted(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    )
}
