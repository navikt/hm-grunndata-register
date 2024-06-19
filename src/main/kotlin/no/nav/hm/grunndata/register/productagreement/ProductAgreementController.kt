package no.nav.hm.grunndata.register.productagreement

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductAgreementController.VENDOR_API_V1_PRODUCT_AGREEMENT)
@Tag(name = "Vendor Product Agreement")
class ProductAgreementController(
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementAdminController::class.java)
        const val VENDOR_API_V1_PRODUCT_AGREEMENT = "/vendor/api/v1/product-agreement"
    }

    @Get(
        value = "/in-agreement/{seriesId}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun isSeriesInAgreement(
        seriesId: UUID,
        authentication: Authentication,
    ): Boolean {
        LOG.info("Checking if series {$seriesId} is part of an agreement")
        val ids: List<UUID> =
            productRegistrationService.findAllBySeriesUuid(seriesId)
                .filter {
                    it.registrationStatus == RegistrationStatus.ACTIVE
                }.map { it.id }

        val productAgreements = productAgreementRegistrationService.findAllByProductIds(ids)

        return productAgreements.isNotEmpty()
    }
}
