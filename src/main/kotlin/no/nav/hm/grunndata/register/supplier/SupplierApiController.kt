package no.nav.hm.grunndata.register.supplier

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierApiController.Companion.API_V1_ADMIN_SUPPLIER_REGISTRATIONS
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_SUPPLIER_REGISTRATIONS)
class SupplierApiController(private val supplierRepository: SupplierRepository) {

    companion object {
        const val API_V1_ADMIN_SUPPLIER_REGISTRATIONS = "/api/v1/admin/supplier/registrations"
        private val LOG = LoggerFactory.getLogger(SupplierApiController::class.java)
    }

    @Get("/{id}")
    suspend fun getById(id: UUID): HttpResponse<SupplierDTO> = supplierRepository.findById(id)?.let {
            HttpResponse.ok(it.toDTO()) } ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createSupplier(@Body supplier: SupplierDTO): HttpResponse<SupplierDTO> =
        supplierRepository.findById(supplier.id)
            ?.let { HttpResponse.badRequest() }
            ?:run { val supplier = supplierRepository.save(supplier.toEntity())
                LOG.info("supplier ${supplier.id} created")
                HttpResponse.created(supplier.toDTO())
            }

    @Put("/{id}")
    suspend fun updateSupplier(@Body supplier: SupplierDTO, id: UUID): HttpResponse<SupplierDTO> =
        supplierRepository.findById(id)
            ?.let { HttpResponse.ok(supplierRepository.update(supplier.toEntity().copy(created = it.created)).toDTO()) }
            ?:run { HttpResponse.notFound() }

    @Delete("/{id}")
    suspend fun deactivateSupplier(id: UUID): HttpResponse<SupplierDTO> =
        supplierRepository.findById(id)
            ?.let { HttpResponse.ok(supplierRepository.update(it.copy(status = SupplierStatus.INACTIVE)).toDTO()) }
            ?:run { HttpResponse.notFound()}

}
