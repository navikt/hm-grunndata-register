package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierAdminApiController.Companion.API_V1_ADMIN_SUPPLIER_REGISTRATIONS
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_SUPPLIER_REGISTRATIONS)
@Tag(name="Admin Supplier")
class SupplierAdminApiController(private val supplierRegistrationService: SupplierRegistrationService) {

    companion object {
        const val API_V1_ADMIN_SUPPLIER_REGISTRATIONS = "/admin/api/v1/supplier/registrations"
        private val LOG = LoggerFactory.getLogger(SupplierAdminApiController::class.java)
    }


    @Get("/{?params*}")
    suspend fun findSuppliers(@QueryValue params: HashMap<String, String>?,
                             pageable: Pageable
    ): Page<SupplierRegistrationDTO> =
        supplierRegistrationService.findAll(params, pageable)


    @Get("/{id}")
    suspend fun getById(id: UUID, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> = supplierRegistrationService.findById(id)?.let {
            HttpResponse.ok(it) } ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createSupplier(@Body supplier: SupplierRegistrationDTO, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> =
        supplierRegistrationService.findByName(supplier.name)
            ?.let { throw BadRequestException("supplier ${supplier.name} already exists") }
            ?:run { val saved = supplierRegistrationService.saveAndCreateEventIfNotDraft(supplier.copy(
                updatedByUser = authentication.name, createdByUser = authentication.name), isUpdate = false)
                HttpResponse.created(saved)
            }

    @Put("/{id}")
    suspend fun updateSupplier(@Body supplier: SupplierRegistrationDTO, id: UUID, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> =
        supplierRegistrationService.findById(id)
            ?.let { inDb -> HttpResponse.ok(supplierRegistrationService.saveAndCreateEventIfNotDraft(
                supplier = supplier.copy(created = inDb.created, identifier = inDb.identifier,
                    createdByUser = inDb.createdByUser, updated = LocalDateTime.now(), updatedByUser = authentication.name),
                isUpdate = true )
            ) } ?:run { HttpResponse.notFound() }

    @Delete("/{id}")
    suspend fun deactivateSupplier(id: UUID, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> =
        supplierRegistrationService.findById(id)
            ?.let { inDb -> HttpResponse.ok(supplierRegistrationService.saveAndCreateEventIfNotDraft (
                supplier = inDb.copy(status = SupplierStatus.INACTIVE),
                isUpdate = true)
            )} ?:run { HttpResponse.notFound()}

}
