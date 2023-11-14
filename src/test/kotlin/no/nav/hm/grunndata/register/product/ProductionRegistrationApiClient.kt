package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Slice
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.MediaType.*
import io.micronaut.http.annotation.*
import no.nav.hm.grunndata.register.CONTEXT_PATH

import java.util.*


@Client("$CONTEXT_PATH/${ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS}")
interface ProductionRegistrationApiClient {

    @Get(uri = "/", consumes = [APPLICATION_JSON])
    fun findProducts(@CookieValue("JWT") jwt: String,
                     @QueryValue("hmsArtNr") hmsArtNr: String?=null,
                     @QueryValue("title") title: String?=null,
                     @QueryValue("size") size: Int? = null,
                     @QueryValue("page") page: Int?=null,
                     @QueryValue("sort") sort: String? = null): Page<ProductRegistrationDTO>

    @Post(uri = "/", processes = [APPLICATION_JSON])
    fun createProduct(@CookieValue("JWT") jwt: String,
                      @Body productRegistrationDTO: ProductRegistrationDTO
    ): ProductRegistrationDTO

    @Get(uri = "/{id}", consumes = [APPLICATION_JSON])
    fun readProduct(@CookieValue("JWT") jwt: String, id: UUID): ProductRegistrationDTO

    @Put(uri= "/{id}", processes = [APPLICATION_JSON])
    fun updateProduct(@CookieValue("JWT") jwt: String, id:UUID,
                      @Body productRegistrationDTO: ProductRegistrationDTO
    ): ProductRegistrationDTO

    @Delete(uri="/{id}", consumes = [APPLICATION_JSON])
    fun deleteProduct(@CookieValue("JWT") jwt: String, id:UUID): ProductRegistrationDTO

    @Get( uri="/series/group", consumes = [APPLICATION_JSON])
    fun findSeriesGroup(@CookieValue("JWT") jwt: String,
                        @QueryValue("size") size: Int? = null,
                        @QueryValue("page") page: Int?=null,
                        @QueryValue("sort") sort: String? = null): Slice<SeriesGroupDTO>

}
