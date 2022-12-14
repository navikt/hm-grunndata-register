package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.MediaType.*
import io.micronaut.http.annotation.*
import java.util.*


@Client(ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS)
interface ProductionRegistrationApiClient {

    @Get(uri = "/", consumes = [APPLICATION_JSON])
    fun findProducts(@CookieValue("JWT") jwt: String, @QueryValue("size") size: Int? = null,
                     @QueryValue("number") number: Int?=null, @QueryValue("sort") sort: String? = null): Page<ProductRegistrationDTO>

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
}
