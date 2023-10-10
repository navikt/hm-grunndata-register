package no.nav.hm.grunndata.register.supplier

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierAdminApiController.Companion.API_V1_ADMIN_SUPPLIER_REGISTRATIONS
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(API_V1_ADMIN_SUPPLIER_REGISTRATIONS)
class SupplierAdminApiController(private val supplierRegistrationService: SupplierRegistrationService) {

    companion object {
        const val API_V1_ADMIN_SUPPLIER_REGISTRATIONS = "/admin/api/v1/supplier/registrations"
        private val LOG = LoggerFactory.getLogger(SupplierAdminApiController::class.java)
    }


    @Get("/{?params*}")
    suspend fun findSuppliers(@QueryValue params: HashMap<String, String>?,
                             pageable: Pageable
    ): Page<SupplierRegistrationDTO> =
        supplierRegistrationService.findAll(buildCriteriaSpec(params), pageable)

    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<SupplierRegistration>? = params?.let {
        where {
            if (params.contains("status")) root[SupplierRegistration::status] eq params["status"]
            if (params.contains("name")) criteriaBuilder.like(root[SupplierRegistration::name], params["name"])
        }
    }


    @Get("/{id}")
    suspend fun getById(id: UUID, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> = supplierRegistrationService.findById(id)?.let {
            HttpResponse.ok(it) } ?: HttpResponse.notFound()

    @Post("/")
    suspend fun createSupplier(@Body supplier: SupplierRegistrationDTO, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> =
        supplierRegistrationService.findByName(supplier.name)
            ?.let { throw BadRequestException("supplier ${supplier.name} already exists") }
            ?:run { val saved = supplierRegistrationService.saveAndPushToRapidIfNotDraft(supplier.copy(
                updatedByUser = authentication.name, createdByUser = authentication.name), isUpdate = false)
                HttpResponse.created(saved)
            }

    @Put("/{id}")
    suspend fun updateSupplier(@Body supplier: SupplierRegistrationDTO, id: UUID, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> =
        supplierRegistrationService.findById(supplier.id)
            ?.let { inDb -> HttpResponse.ok(supplierRegistrationService.saveAndPushToRapidIfNotDraft(
                supplier = supplier.copy(created = inDb.created, identifier = inDb.identifier,
                    createdByUser = inDb.createdByUser, updated = LocalDateTime.now(), updatedByUser = authentication.name),
                isUpdate = true )
            ) } ?:run { HttpResponse.notFound() }

    @Delete("/{id}")
    suspend fun deactivateSupplier(id: UUID, authentication: Authentication): HttpResponse<SupplierRegistrationDTO> =
        supplierRegistrationService.findById(id)
            ?.let { inDb -> HttpResponse.ok(supplierRegistrationService.saveAndPushToRapidIfNotDraft (
                supplier = inDb.copy(status = SupplierStatus.INACTIVE),
                isUpdate = true)
            )} ?:run { HttpResponse.notFound()}

}
