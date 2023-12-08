package no.nav.hm.grunndata.register.productagreement

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.userId
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductAgreementAdminController.ADMIN_API_V1_PRODUCT_AGREEMENT)
class ProductAgreementAdminController(private val productAgreementImportExcelService: ProductAgreementImportExcelService,
                                      private val productRegistrationService: ProductRegistrationService,
                                      private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementAdminController::class.java)
        const val ADMIN_API_V1_PRODUCT_AGREEMENT = "/admin/api/v1/product-agreement"
    }


    @Post(  value = "/excel-import",
            consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
            produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun excelImport(file: CompletedFileUpload,
                            @QueryValue dryRun: Boolean = true,
                            authentication: Authentication): ProductAgreementImportDTO {
        LOG.info("Importing excel file: ${file.filename}, dryRun: $dryRun by ${authentication.userId()}")
        val productAgreements = file.inputStream.use { input -> productAgreementImportExcelService.importExcelFile(input) }
        if (!dryRun) {
            LOG.info("Saving product agreements")
            productAgreementRegistrationService.saveAll(productAgreements)
        }
        return ProductAgreementImportDTO(dryRun = dryRun, count = productAgreements.size, productAgreements = productAgreements)
    }


    @Put(  value = "/products/connect/{agreementId}",
            consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
            produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun connectProductsToAgreement(agreementId: UUID): List<ProductAgreementRegistrationDTO> {
        LOG.info("Connecting products to agreement: $agreementId")
        val products = productAgreementRegistrationService.findByAgreementId(agreementId).filter { it.productId == null }
        LOG.info("Got ${products.size} products to connect")
        return products.map {
            val product = productRegistrationService.findBySupplierRefAndSupplierId(it.supplierRef, it.supplierId)
            it.copy(productId = product?.id)
        }
    }
}