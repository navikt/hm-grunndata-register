package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.CONTEXT_PATH
import java.util.*


@Client("$CONTEXT_PATH/${ProductRegistrationAdminApiController.API_V1_ADMIN_PRODUCT_REGISTRATIONS}")
interface ProductRegistrationAdminApiClient {

    @Get(uri = "/", consumes = [APPLICATION_JSON])
    fun findProducts(@CookieValue("JWT") jwt: String,
                     @QueryValue supplierId: UUID? = null,
                     @QueryValue adminStatus: AdminStatus? = null,
                     @QueryValue draftStatus: DraftStatus? = null,
                     @QueryValue supplierRef: String? = null,
                     @QueryValue createdByUser: String? = null,
                     @QueryValue updatedByUser: String? = null,
                     @QueryValue("size") size: Int? = null,
                     @QueryValue("page") page: Int?=null,
                     @QueryValue("sort") sort: String? = null): Page<ProductRegistrationDTO>

    @Post(uri = "/", processes = [APPLICATION_JSON])
    fun createProduct(@CookieValue("JWT") jwt: String,
                      @Body productRegistrationDTO: ProductRegistrationDTO
    ): ProductRegistrationDTO

    @Get(uri = "/{id}", produces = [APPLICATION_JSON])
    fun readProduct(@CookieValue("JWT") jwt: String, id: UUID): ProductRegistrationDTO

    @Put(uri= "/v2/{id}", processes = [APPLICATION_JSON])
    fun updateProduct(@CookieValue("JWT") jwt: String, id:UUID,
                      @Body updateProductRegistrationDTO: UpdateProductRegistrationDTO
    ): ProductRegistrationDTO

    @Delete(uri="/{id}", consumes = [APPLICATION_JSON])
    fun deleteProduct(@CookieValue("JWT") jwt: String, id:UUID): ProductRegistrationDTO

    @Post(uri="/draft/supplier/{supplierId}", produces = [APPLICATION_JSON])
    fun draftProduct(@CookieValue("JWT") jwt: String, supplierId: UUID):ProductRegistrationDTO


}
