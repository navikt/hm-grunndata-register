package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.UUID

@Client("$CONTEXT_PATH/${ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS}")
interface ProductRegistrationApiClient {
    @Get(uri = "/series/{seriesUUID}", consumes = [APPLICATION_JSON])
    fun findBySeriesUUIDAndSupplierId(
        @CookieValue("JWT") jwt: String,
        @PathVariable seriesUUID: UUID,
    ): List<ProductRegistrationDTO>

    @Get(uri = "/", consumes = [APPLICATION_JSON])
    fun findProducts(
        @CookieValue("JWT") jwt: String,
        @QueryValue("hmsArtNr") hmsArtNr: String? = null,
        @QueryValue("supplierRef") supplierRef: String? = null,
        @QueryValue("registrationStatus") registrationStatus: String? = null,
        @QueryValue("title") title: String? = null,
        @QueryValue("size") size: Int? = null,
        @QueryValue("page") page: Int? = null,
        @QueryValue("sort") sort: String? = null,
    ): Page<ProductRegistrationDTO>

    @Post(uri = "/draftWithV3/{seriesUUID}", processes = [APPLICATION_JSON])
    fun createDraft(
        @CookieValue("JWT") jwt: String,
        @PathVariable seriesUUID: UUID,
        @Body draftVariantDTO: DraftVariantDTO,
    ): ProductRegistrationDTO

    @Get(uri = "/{id}", consumes = [APPLICATION_JSON])
    fun readProduct(
        @CookieValue("JWT") jwt: String,
        id: UUID,
    ): ProductRegistrationDTO

    @Put(uri = "/v2/{id}", processes = [APPLICATION_JSON])
    fun updateProduct(
        @CookieValue("JWT") jwt: String,
        id: UUID,
        @Body updateProductRegistrationDTO: UpdateProductRegistrationDTO
    ): ProductRegistrationDTO

    @Delete(uri = "/draft/delete", consumes = [APPLICATION_JSON])
    fun deleteDraftVariants(
        @CookieValue("JWT") jwt: String,
        @Body ids: List<UUID>,
    )
}
