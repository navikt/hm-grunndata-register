package no.nav.hm.grunndata.register.product.version

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.version.Difference
import org.slf4j.LoggerFactory


@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationVersionAdminController.API_V1_PRODUCT_VERSIONS)
@Tag(name = "Admin Products")
class ProductRegistrationVersionAdminController(private val productRegistrationVersionService: ProductRegistrationVersionService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductRegistrationVersionAdminController::class.java)
        const val API_V1_PRODUCT_VERSIONS = "/admin/api/v1/product/versions"
    }

    @Get("/")
    suspend fun getSeriesVersions(
        @RequestBean criteria: ProductRegistrationVersionCriteria,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<ProductRegistrationVersionDTO> {
        return productRegistrationVersionService.findAll(buildCriteriaSpec(criteria), pageable).map { it.toDTO() }
    }

    private fun buildCriteriaSpec(criteria: ProductRegistrationVersionCriteria): PredicateSpecification<ProductRegistrationVersion>? =
        if (criteria.isNotEmpty()) {
            where {
                criteria.productId?.let { root[ProductRegistrationVersion::productId] eq it  }
                criteria.version?.let { root[ProductRegistrationVersion::version] eq it }
                criteria.status?.let { root[ProductRegistrationVersion::status] eq it }
                criteria.adminStatus?.let { root[ProductRegistrationVersion::adminStatus] eq it }
                criteria.draftStatus?.let { root[ProductRegistrationVersion::draftStatus] eq it }
            }
        } else null

    @Get("/{productId}/compare/{version}/approved")
    suspend fun compareVersionWithApproved(
        productId: UUID,
        version: Long,
        authentication: Authentication,
    ): HttpResponse<Difference<String, Any>> {
        val productVersion = productRegistrationVersionService.findByProductIdAndVersion(productId, version)
            ?: return HttpResponse.notFound()
        return HttpResponse.ok(productRegistrationVersionService.diffWithLastApprovedVersion(productVersion))
    }

}

@Introspected
data class ProductRegistrationVersionCriteria(
    val productId: UUID? = null,
    val version: Long? = null,
    val status: RegistrationStatus? = null,
    val adminStatus: AdminStatus? = null,
    val draftStatus: DraftStatus? = null,
) {
    fun isNotEmpty(): Boolean = productId != null || version != null || status != null || adminStatus != null || draftStatus != null
}