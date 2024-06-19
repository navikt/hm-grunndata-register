package no.nav.hm.grunndata.register.product

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
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

    @Get("/{?params*}")
    suspend fun getSeriesVersions(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<ProductRegistrationVersionDTO> {
        return productRegistrationVersionService.findAll(buildCriteriaSpec(params), pageable).map { it.toDTO() }
    }

    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<ProductRegistrationVersion>? =
        params?.let {
            where {
                if (it.containsKey("productId")) root[ProductRegistrationVersion::productId] eq it["productId"]
                if (it.containsKey("version")) root[ProductRegistrationVersion::version] eq it["version"]
                if (it.containsKey("status")) root[ProductRegistrationVersion::status] eq RegistrationStatus.valueOf(it["status"]!!)
                if (it.containsKey("adminStatus")) root[ProductRegistrationVersion::adminStatus] eq AdminStatus.valueOf(it["adminStatus"]!!)
                if (it.containsKey("draftStatus")) root[ProductRegistrationVersion::draftStatus] eq DraftStatus.valueOf(it["draftStatus"]!!)
            }
        }

    @Get("/{productId}/compare/{version}/approved")
    suspend fun compareVersionWithApproved(
        productId: UUID,
        version: Long,
        authentication: Authentication,
    ): HttpResponse<Difference<String, Any>> {
        val productVersion = productRegistrationVersionService.findByProductIdAndVersion(productId, version)
        val approvedVersion = productRegistrationVersionService.findLastApprovedVersion(productId)
        if (productVersion!=null && approvedVersion!=null) {
            return HttpResponse.ok(productRegistrationVersionService.diffVersions(productVersion, approvedVersion))
        }
        else (
            return HttpResponse.notFound()
        )
    }



}