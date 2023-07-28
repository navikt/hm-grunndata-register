package no.nav.hm.grunndata.register.supplier

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_SUPPLIER)
@Controller(SupplierApiController.API_V1_SUPPLIER_REGISTRATIONS)
class SupplierApiController(private val supplierRegistrationService: SupplierRegistrationService) {

    companion object {
        const val API_V1_SUPPLIER_REGISTRATIONS = "/api/v1/supplier/registrations"
        private val LOG = LoggerFactory.getLogger(SupplierApiController::class.java)
    }

    @Get("/")
    suspend fun getById(authentication: Authentication): HttpResponse<SupplierRegistrationDTO> {
        return supplierRegistrationService.findById(authentication.supplierId())?.let {
            HttpResponse.ok(it)
        } ?: HttpResponse.notFound()
    }


    @Put("/")
    suspend fun updateSupplier(@Body supplier: SupplierRegistrationDTO, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> {
        if (supplier.id != authentication.supplierId()) {
            LOG.error("user made an unauthorized request for supplier ${supplier.id}")
            return HttpResponse.unauthorized()
        }
        return supplierRegistrationService.findById(authentication.supplierId())
            ?.let { inDb -> HttpResponse.ok(supplierRegistrationService.saveAndPushToRapidIfNotDraft(
                supplier.copy(
                    status = inDb.status, created = inDb.created, identifier = inDb.identifier,
                    updated = LocalDateTime.now()), isUpdate = true))
            }
            ?:run { HttpResponse.notFound() }
    }

}
