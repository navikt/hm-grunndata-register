package no.nav.hm.grunndata.register.product.workswith

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.product.isSupplier
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException

@Secured(Roles.ROLE_HMS, Roles.ROLE_ADMIN, Roles.ROLE_SUPPLIER)
@Controller(WorksWithController.API_V1_WORKSWITH_REGISTRATIONS)
@Tag(name = "WorksWith API")
class WorksWithController(
    private val worksWithConnector: WorksWithConnector,
    private val productRegistrationService: ProductRegistrationService
) {

    companion object {
        const val API_V1_WORKSWITH_REGISTRATIONS = "/vendor/api/v1/works-with"
        private val LOG = LoggerFactory.getLogger(WorksWithController::class.java)
    }

    @Post("/")
    suspend fun createWorksWithRelations(
        @Body worksWithMapping: WorksWithMapping,
        authentication: Authentication
    ): ProductRegistration {
        val sourceProductId = worksWithMapping.sourceProductId
        val targetProductId = worksWithMapping.targetProductId
        if (authentication.isSupplier()) {
            val sourceProduct = productRegistrationService.findByIdAndSupplierId(
                sourceProductId,
                authentication.supplierId()
            ) ?: throw IllegalArgumentException("Product not found for id: ${sourceProductId}")
        }
        return worksWithConnector.addConnection(worksWithMapping)
    }

    @Delete("/")
    suspend fun deleteWorksWithRelations(
        @Body worksWithMapping: WorksWithMapping,
        authentication: Authentication): ProductRegistration {
        val sourceProductId = worksWithMapping.sourceProductId
        val targetProductId = worksWithMapping.targetProductId
        if (authentication.isSupplier()) {
            val sourceProduct = productRegistrationService.findByIdAndSupplierId(sourceProductId, authentication.supplierId()) ?:
                throw IllegalArgumentException("Product not found for id: ${sourceProductId}")
        }
        return worksWithConnector.removeConnection(worksWithMapping)
    }
}