package no.nav.hm.grunndata.register.product

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_HMS, Roles.ROLE_ADMIN)
@Controller(PartRegistrationApiController.API_V1_PART_REGISTRATIONS)
@Tag(name = "HMS-user and admin Parts")
class PartRegistrationApiController(
    private val productRegistrationService: ProductRegistrationService,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V1_PART_REGISTRATIONS = "/api/v1/part/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationAdminApiController::class.java)
    }

    @Get("/")
    suspend fun findParts(
        @RequestBean criteria: ProductRegistrationHmsUserCriteria,
        pageable: Pageable,
    ): Page<ProductRegistrationDTOV2> = productRegistrationService
        .findAll(buildCriteriaSpec(criteria), pageable)
        .mapSuspend { productDTOMapper.toDTOV2(it) }

    private fun buildCriteriaSpec(criteria: ProductRegistrationHmsUserCriteria): PredicateSpecification<ProductRegistration>? =
        where {
            criteria.supplierRef?.let { root[ProductRegistration::supplierRef] eq it }
            criteria.hmsArtNr?.let { root[ProductRegistration::hmsArtNr] eq it }
            criteria.title?.let { root[ProductRegistration::title] like LiteralExpression("%${it}%") }
            or {
                root[ProductRegistration::accessory] eq true
                root[ProductRegistration::sparePart] eq true
            }
        }

}

@Introspected
data class ProductRegistrationHmsUserCriteria(
    val supplierRef: String? = null,
    val hmsArtNr: String? = null,
    val adminStatus: AdminStatus? = null,
    val registrationStatus: RegistrationStatus? = null,
    val supplierId: UUID? = null,
    val draft: DraftStatus? = null,
    val createdByUser: String? = null,
    val updatedByUser: String? = null,
    val title: String? = null,
) {
    fun isNotEmpty(): Boolean = listOfNotNull(
        supplierRef, hmsArtNr, adminStatus, registrationStatus, supplierId, draft, createdByUser, updatedByUser, title
    ).isNotEmpty()
}

