package no.nav.hm.grunndata.register.productagreement

import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.userId
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.HashMap

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductAgreementAdminController.ADMIN_API_V1_PRODUCT_AGREEMENT)
class ProductAgreementAdminController(private val productAgreementImportExcelService: ProductAgreementImportExcelService,
                                      private val productRegistrationService: ProductRegistrationService,
                                      private val agreementRegistrationService: AgreementRegistrationService,
                                      private val productAgreementRegistrationService: ProductAgreementRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementAdminController::class.java)
        const val ADMIN_API_V1_PRODUCT_AGREEMENT = "/admin/api/v1/product-agreement"
    }


    @Post(
        value = "/excel-import",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun excelImport(
        file: CompletedFileUpload,
        @QueryValue dryRun: Boolean = true,
        authentication: Authentication
    ): ProductAgreementImportDTO {
        LOG.info("Importing excel file: ${file.filename}, dryRun: $dryRun by ${authentication.userId()}")
        val productAgreements =
            file.inputStream.use { input -> productAgreementImportExcelService.importExcelFile(input) }
        if (!dryRun) {
            LOG.info("Saving product agreements")
            productAgreementRegistrationService.saveAll(productAgreements)
        }
        return ProductAgreementImportDTO(
            dryRun = dryRun,
            count = productAgreements.size,
            productAgreements = productAgreements
        )
    }


    @Post(
        value = "/",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON]
    )
    suspend fun createProductAgreement(
        @Body regDTO: ProductAgreementRegistrationDTO,
        authentication: Authentication
    ): ProductAgreementRegistrationDTO {
        LOG.info("Creating product agreement: ${regDTO.agreementId} ${regDTO.supplierId} ${regDTO.supplierRef} by ${authentication.userId()}")
        productAgreementRegistrationService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostAndRank(
            regDTO.supplierId,
            regDTO.supplierRef,
            regDTO.agreementId,
            regDTO.post,
            regDTO.rank
        )?.let {
            throw BadRequestException("Product agreement already exists")
        }
        val product = productRegistrationService.findBySupplierRefAndSupplierId(regDTO.supplierRef, regDTO.supplierId)
            ?: throw BadRequestException("Product not found")
        val agreement = agreementRegistrationService.findById(regDTO.agreementId)
            ?: throw BadRequestException("Agreement ${regDTO.agreementId} not found")
        return productAgreementRegistrationService.saveAndCreateEvent(
            ProductAgreementRegistrationDTO(
                supplierRef = regDTO.supplierRef,
                supplierId = regDTO.supplierId,
                agreementId = regDTO.agreementId,
                post = regDTO.post,
                rank = regDTO.rank,
                createdBy = "REGISTER",
                published = agreement.published,
                expired = agreement.expired,
                hmsArtNr = product.hmsArtNr,
                productId = product.id,
                title = regDTO.title,
                reference = agreement.reference
            ), isUpdate = false
        )
    }

    @Delete("/{id}")
    suspend fun deleteProductAgreementById(id: UUID, authentication: Authentication) {
        LOG.info("deleting product agreement: $id by ${authentication.userId()}")
        productAgreementRegistrationService.findById(id)?.let {
            productAgreementRegistrationService.saveAndCreateEvent(it.copy(status = ProductAgreementStatus.DELETED), isUpdate = true)
        } ?: throw BadRequestException("Product agreement $id not found")
    }

    @Delete("/ids")
    suspend fun deleteProductAgreementByIds(@Body ids: List<UUID>, authentication: Authentication) {
        LOG.info("deleting product agreements: $ids by ${authentication.userId()}")
        ids.forEach {
            productAgreementRegistrationService.findById(it)?.let {
                productAgreementRegistrationService.saveAndCreateEvent(it.copy(status = ProductAgreementStatus.DELETED), isUpdate = true)
            } ?: throw BadRequestException("Product agreement $it not found")
        }
    }

}

