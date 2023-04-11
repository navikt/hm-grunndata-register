package no.nav.hm.grunndata.register.supplier

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(SupplierApiController.API_V1_SUPPLIER_REGISTRATIONS)
class SupplierApiController(private val supplierService: SupplierService) {

    companion object {
        const val API_V1_SUPPLIER_REGISTRATIONS = "/api/v1/supplier/registrations"
        private val LOG = LoggerFactory.getLogger(SupplierApiController::class.java)
    }

    @Get("/")
    suspend fun getById(authentication: Authentication): HttpResponse<SupplierDTO> =
        supplierService.findById(authentication.supplierId())?.let {
            HttpResponse.ok(it.toDTO())
        } ?: HttpResponse.notFound()


    @Put("/")
    suspend fun updateSupplier(@Body supplier: SupplierDTO, authentication: Authentication): HttpResponse<SupplierDTO> =
        supplierService.findById(authentication.supplierId())
            ?.let { HttpResponse.ok(supplierService.update(supplier.toEntity()
                // supplier can not change its status
                .copy(status = it.status, created = it.created, identifier = it.identifier, updated = LocalDateTime.now())).toDTO()) }
            ?:run { HttpResponse.notFound() }

}
