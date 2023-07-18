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
class SupplierAdminApiController(private val supplierService: SupplierService) {

    companion object {
        const val API_V1_ADMIN_SUPPLIER_REGISTRATIONS = "/api/v1/admin/supplier/registrations"
        private val LOG = LoggerFactory.getLogger(SupplierAdminApiController::class.java)
    }

    @Get("/{id}")
    suspend fun getById(id: UUID): HttpResponse<SupplierDTO> = supplierService.findById(id)?.let {
            HttpResponse.ok(it.toRapidDTO()) } ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createSupplier(@Body supplier: SupplierDTO): HttpResponse<SupplierDTO> =
        supplierService.findById(supplier.id)
            ?.let { throw BadRequestException("supplier ${supplier.id} already exists") }
            ?:run { val saved = supplierService.save(supplier.toEntity())
                LOG.info("supplier ${saved.id} created")
                HttpResponse.created(saved.toRapidDTO())
            }

    @Put("/{id}")
    suspend fun updateSupplier(@Body supplier: SupplierDTO, id: UUID): HttpResponse<SupplierDTO> =
        supplierService.findById(id)
            ?.let { HttpResponse.ok(supplierService.update(supplier.toEntity()
                //identifier can not be changed during migration
                .copy(created = it.created, identifier = it.identifier, updated = LocalDateTime.now())).toRapidDTO()) }
            ?:run { HttpResponse.notFound() }

    @Delete("/{id}")
    suspend fun deactivateSupplier(id: UUID): HttpResponse<SupplierDTO> =
        supplierService.findById(id)
            ?.let { HttpResponse.ok(supplierService.update(it.copy(status = SupplierStatus.INACTIVE)).toRapidDTO()) }
            ?:run { HttpResponse.notFound()}

}
