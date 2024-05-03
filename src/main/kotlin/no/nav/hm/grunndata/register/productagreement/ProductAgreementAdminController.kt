package no.nav.hm.grunndata.register.productagreement

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.userId
import org.slf4j.LoggerFactory
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductAgreementAdminController.ADMIN_API_V1_PRODUCT_AGREEMENT)
@Tag(name = "Admin Product Agreement")
class ProductAgreementAdminController(
    private val productAgreementImportExcelService: ProductAgreementImportExcelService,
    private val productRegistrationService: ProductRegistrationService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementAdminController::class.java)
        const val ADMIN_API_V1_PRODUCT_AGREEMENT = "/admin/api/v1/product-agreement"
    }

    @Post(
        value = "/excel-import",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun excelImport(
        file: CompletedFileUpload,
        @QueryValue dryRun: Boolean = true,
        authentication: Authentication,
    ): ProductAgreementImportDTO {
        LOG.info("Importing excel file: ${file.filename}, dryRun: $dryRun by ${authentication.userId()}")
        val productAgreements =
            file.inputStream.use { input -> productAgreementImportExcelService.importExcelFile(input) }

        val productAgreementsWithInformation =
            productAgreements.map {
                val information = mutableListOf<Information>()
                val existingProductAgreement =
                    productAgreementRegistrationService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
                        it.supplierId,
                        it.supplierRef,
                        it.agreementId,
                        it.postId!!,
                        it.rank,
                    )

                if (existingProductAgreement != null) {
                    information.add(Information("Product agreement already exists", Type.WARNING))
                }
                Pair(it, information)
            }

        if (!dryRun) {
            LOG.info("Saving product agreements")
            productAgreementRegistrationService.saveAll(productAgreements)
        }
        return ProductAgreementImportDTO(
            dryRun = dryRun,
            count = productAgreements.size,
            productAgreements = productAgreements,
            productAgreementsWithInformation = productAgreementsWithInformation,
        )
    }

    @Get(
        value = "/{id}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun getProductsByAgreementId(
        id: UUID,
        authentication: Authentication,
    ): List<ProductAgreementRegistrationDTO> {
        LOG.info("Getting products for agreement {$id} by ${authentication.userId()}")
        return productAgreementRegistrationService.findByAgreementId(id)
    }

    @Post(
        value = "/get-by-ids",
        consumes = [io.micronaut.http.MediaType.APPLICATION_JSON],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun getProductsAgreementsByIds(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): List<ProductAgreementRegistrationDTO> {
        LOG.info("Getting productsAgreements by ${authentication.userId()}")
        return productAgreementRegistrationService.findAllByIds(ids)
    }

    @Get(
        value = "/variants/{id}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun getProductVariantsByAgreementId(
        id: UUID,
        authentication: Authentication,
    ): List<ProduktvarianterForDelkontrakterDTO> {
        LOG.info("Getting product variants for agreement {$id} by ${authentication.userId()}")
        return productAgreementRegistrationService.findGroupedProductVariantsByAgreementId(id)
    }

    @Get(
        value = "/variants/delkontrakt/{id}",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun getProductVariantsByDelkontraktId(
        id: UUID,
        authentication: Authentication,
    ): List<ProductVariantsForDelkontraktDto> {
        LOG.info("Getting product variants for delkontrakt {$id} by ${authentication.userId()}")
        return productAgreementRegistrationService.findGroupedProductVariantsByDelkontraktId(id)
    }

    @Post(
        value = "/",
        consumes = [io.micronaut.http.MediaType.MULTIPART_FORM_DATA],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun createProductAgreement(
        @Body regDTO: ProductAgreementRegistrationDTO,
        authentication: Authentication,
    ): ProductAgreementRegistrationDTO {
        LOG.info(
            "Creating product agreement: ${regDTO.agreementId} ${regDTO.supplierId} ${regDTO.supplierRef} by ${authentication.userId()}",
        )
        productAgreementRegistrationService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
            regDTO.supplierId,
            regDTO.supplierRef,
            regDTO.agreementId,
            regDTO.postId!!,
            regDTO.rank,
        )?.let {
            throw BadRequestException("Product agreement already exists")
        }
        val product =
            productRegistrationService.findBySupplierRefAndSupplierId(regDTO.supplierRef, regDTO.supplierId)
                ?: throw BadRequestException("Product not found")
        val agreement =
            agreementRegistrationService.findById(regDTO.agreementId)
                ?: throw BadRequestException("Agreement ${regDTO.agreementId} not found")
        return productAgreementRegistrationService.saveAndCreateEvent(
            ProductAgreementRegistrationDTO(
                supplierRef = regDTO.supplierRef,
                supplierId = regDTO.supplierId,
                agreementId = regDTO.agreementId,
                post = regDTO.post,
                rank = regDTO.rank,
                postId = regDTO.postId,
                createdBy = "REGISTER",
                published = agreement.published,
                expired = agreement.expired,
                hmsArtNr = product.hmsArtNr,
                productId = product.id,
                seriesUuid = product.seriesUUID,
                articleName = product.articleName,
                title = regDTO.title,
                reference = agreement.reference,
            ),
            isUpdate = false,
        )
    }

    @Post(
        value = "/batch",
        consumes = [io.micronaut.http.MediaType.APPLICATION_JSON],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun createProductAgreements(
        @Body regDTOs: List<ProductAgreementRegistrationDTO>,
        authentication: Authentication,
    ): List<ProductAgreementRegistrationDTO> {
        LOG.info("Creating ${regDTOs.size} product agreements by ${authentication.userId()}")
        val lagrede =
            productAgreementRegistrationService.saveOrUpdateAll(
                regDTOs,
            )
        return lagrede
    }

    @Put(
        value = "/batch",
        consumes = [io.micronaut.http.MediaType.APPLICATION_JSON],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun updateProductAgreements(
        @Body regDTOs: List<ProductAgreementRegistrationDTO>,
        authentication: Authentication,
    ): List<ProductAgreementRegistrationDTO> {
        LOG.info("Updating ${regDTOs.size} product agreements by ${authentication.userId()}")
        val lagrede =
            productAgreementRegistrationService.updateAll(
                regDTOs,
            )
        return lagrede
    }

    @Delete("/{id}")
    suspend fun deleteProductAgreementById(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductAgreementDeletedResponse> {
        LOG.info("deleting product agreement: $id by ${authentication.userId()}")
        productAgreementRegistrationService.deleteById(id)
        return HttpResponse.ok(ProductAgreementDeletedResponse(id))
    }

    @Delete(
        value = "/ids",
        consumes = [io.micronaut.http.MediaType.APPLICATION_JSON],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun deleteProductAgreementByIds(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<ProductAgreementsDeletedResponse> {
        LOG.info("deleting product agreements: $ids by ${authentication.userId()}")
        productAgreementRegistrationService.deleteByIds(ids)
        return HttpResponse.ok(ProductAgreementsDeletedResponse(ids))
    }
}

data class ProductAgreementsDeletedResponse(
    val ids: List<UUID>,
)

data class ProductAgreementDeletedResponse(
    val id: UUID,
)
