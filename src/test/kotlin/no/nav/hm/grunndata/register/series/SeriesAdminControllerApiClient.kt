package no.nav.hm.grunndata.register.series

import io.micronaut.data.model.Page
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.UUID

@Client("$CONTEXT_PATH/${SeriesRegistrationAdminController.API_V1_SERIES}")
interface SeriesAdminControllerApiClient {

    @Put(uri = "/approve-v2/{id}", processes = [APPLICATION_JSON])
    fun approveSeries(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    )

    @Put(uri = "/approve-multiple", processes = [APPLICATION_JSON])
    fun approveMultipleSeries(
        @CookieValue("JWT") jwt: String,
        @Body ids: List<UUID>,
    )

    @Put(uri = "/reject-v2/{id}", processes = [APPLICATION_JSON])
    fun rejectSeries(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
        @Body rejectSeriesDTO: RejectSeriesDTO
    )

    @Get(uri = "/to-approve", processes = [APPLICATION_JSON])
    fun findSeriesPendingApproval(
        @CookieValue("JWT") jwt: String,
    ): Page<SeriesToApproveDTO>

    @Get(uri = "/supplier-inventory/{id}", processes = [APPLICATION_JSON])
    fun getSupplierInventory(
        @CookieValue("JWT") jwt: String,
        @PathVariable id: UUID,
    ): SupplierInventoryDTO

    @Put(uri = "/series/products/move-to/{seriesId}", processes = [APPLICATION_JSON])
    fun moveProductVariantsToSeries(
        @CookieValue("JWT") jwt: String,
        @PathVariable seriesId: UUID,
        productIds: List<UUID>,
    )
}
