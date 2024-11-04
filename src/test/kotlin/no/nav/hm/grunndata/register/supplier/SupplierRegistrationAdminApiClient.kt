package no.nav.hm.grunndata.register.supplier

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
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.CONTEXT_PATH

@Client("$CONTEXT_PATH/${SupplierAdminApiController.API_V1_ADMIN_SUPPLIER_REGISTRATIONS}")
interface SupplierRegistrationAdminApiClient {

    @Get(uri = "/", consumes = [APPLICATION_JSON])
    fun findSuppliers(@CookieValue("JWT") jwt: String,
                      @QueryValue status: SupplierStatus? = null,
                      @QueryValue name: String? = null,
                      @QueryValue("size") size: Int? = null,
                      @QueryValue("page") page: Int?=null,
                      @QueryValue("sort") sort: String? = null): Page<SupplierRegistrationDTO>

    @Post
    fun createSupplier(@CookieValue("JWT") jwt: String, @Body supplierRegistrationDTO: SupplierRegistrationDTO): SupplierRegistrationDTO

    @Get(uri = "/{id}", produces = [APPLICATION_JSON])
    fun readSupplier(@CookieValue("JWT") jwt: String, id: UUID): SupplierRegistrationDTO

    @Put(uri= "/{id}", processes = [APPLICATION_JSON])
    fun updateSupplier(@CookieValue("JWT") jwt: String, id:UUID,
                       @Body updateSupplierRegistrationDTO: SupplierRegistrationDTO
    ): SupplierRegistrationDTO

    @Delete(uri="/delete", consumes = [APPLICATION_JSON])
    fun deleteSupplier(@CookieValue("JWT") jwt: String, @Body ids: List<UUID>): SupplierRegistrationDTO

}