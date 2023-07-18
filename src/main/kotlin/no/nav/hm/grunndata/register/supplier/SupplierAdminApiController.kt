package no.nav.hm.grunndata.register.supplier

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.api.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierAdminApiController.Companion.API_V1_ADMIN_SUPPLIER_REGISTRATIONS
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_SUPPLIER_REGISTRATIONS)
class SupplierAdminApiController(private val supplierService: SupplierService,
                                 private val supplierRegistrationHandler: SupplierRegistrationHandler) {

    companion object {
        const val API_V1_ADMIN_SUPPLIER_REGISTRATIONS = "/api/v1/admin/supplier/registrations"
        private val LOG = LoggerFactory.getLogger(SupplierAdminApiController::class.java)
    }

    @Get("/{id}")
    suspend fun getById(id: UUID): HttpResponse<SupplierRegistrationDTO> = supplierService.findById(id)?.let {
            HttpResponse.ok(it) } ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createSupplier(@Body supplier: SupplierRegistrationDTO): HttpResponse<SupplierRegistrationDTO> =
        supplierService.findById(supplier.id)
            ?.let { throw BadRequestException("supplier ${supplier.id} already exists") }
            ?:run { val saved = supplierService.saveAndPushToKafka(supplier, isUpdate = false)
                HttpResponse.created(saved)
            }

    @Put("/{id}")
    suspend fun updateSupplier(@Body supplier: SupplierRegistrationDTO, id: UUID): HttpResponse<SupplierRegistrationDTO> =
        supplierService.findById(id)
            ?.let { HttpResponse.ok(supplierService.saveAndPushToKafka(
                supplier = supplier.copy(created = it.created, identifier = it.identifier, createdByUser = it.createdByUser,
                    updated = LocalDateTime.now()),
                isUpdate = true )
            ) } ?:run { HttpResponse.notFound() }

    @Delete("/{id}")
    suspend fun deactivateSupplier(id: UUID): HttpResponse<SupplierRegistrationDTO> =
        supplierService.findById(id)
            ?.let { HttpResponse.ok(supplierService.saveAndPushToKafka (
                supplier = it.copy(status = SupplierStatus.INACTIVE),
                isUpdate = true)
            )} ?:run { HttpResponse.notFound()}

}
