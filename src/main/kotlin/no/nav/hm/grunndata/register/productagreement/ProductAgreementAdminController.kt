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
import io.micronaut.transaction.annotation.Transactional
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.userId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.register.catalog.CatalogExcelFileImport
import no.nav.hm.grunndata.register.catalog.CatalogImportResult
import no.nav.hm.grunndata.register.catalog.CatalogImportService

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductAgreementAdminController.ADMIN_API_V1_PRODUCT_AGREEMENT)
@Tag(name = "Admin Product Agreement")
open class ProductAgreementAdminController(
    private val catalogExcelFileImport: CatalogExcelFileImport,
    private val productAgreementImportExcelService: ProductAgreementImportExcelService,
    private val productRegistrationService: ProductRegistrationService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
    private val productAccessorySparePartAgreementHandler: ProductAccessorySparePartAgreementHandler,
    private val catalogImportService: CatalogImportService
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
        val importedExcelCatalog =
            file.inputStream.use { input -> catalogExcelFileImport.importExcelFile(input) }
        val catalogImportResult = catalogImportService.prepareCatalogImportResult(importedExcelCatalog.map { it.toEntity() })
        val mappedCatalogImportResult = productAgreementImportExcelService.mapCatalogImport(catalogImportResult, authentication)

        val productAgreementsImportResult =
            productAccessorySparePartAgreementHandler.handleNewProductsInExcelImport(
                mappedCatalogImportResult,
                authentication,
                dryRun,
            )
        val productAgreements = productAgreementsImportResult.insertList
        LOG.info("New product agreements found: ${productAgreements.size}")
        val newCount = productAgreementsImportResult.insertList.size

        if (!dryRun) {
            persistCatalogResult(catalogImportResult, file, productAgreementsImportResult)
        }

        return ProductAgreementImportDTO(
            dryRun = dryRun,
            count = productAgreements.size,
            newCount = newCount,
            file = file.filename,
            createdSeries = productAgreementsImportResult.newSeries,
            createdAccessoryParts = productAgreementsImportResult.newAccessoryParts,
            createdMainProducts = productAgreementsImportResult.newProducts,
            newProductAgreements = productAgreementsImportResult.insertList,
            updatedAgreements = mappedCatalogImportResult.updateList,
            deactivatedAgreements = mappedCatalogImportResult.deactivateList,
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

        val status =
            if (agreement.draftStatus != DraftStatus.DRAFT &&
                agreement.published < LocalDateTime.now() &&
                agreement.expired > LocalDateTime.now()
            ) {
                ProductAgreementStatus.ACTIVE
            } else {
                ProductAgreementStatus.INACTIVE
            }

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
                updatedBy = REGISTER,
                accessory = product.accessory,
                sparePart = product.sparePart,
                isoCategory = product.isoCategory,
                status = status,
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

        val validated =
            regDTOs.map { regDTO ->
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

                val status =
                    if (agreement.draftStatus != DraftStatus.DRAFT &&
                        agreement.published < LocalDateTime.now() &&
                        agreement.expired > LocalDateTime.now()
                    ) {
                        ProductAgreementStatus.ACTIVE
                    } else {
                        ProductAgreementStatus.INACTIVE
                    }

                regDTO.copy(
                    status = status,
                    createdBy = "REGISTER",
                    published = agreement.published,
                    expired = agreement.expired,
                    hmsArtNr = product.hmsArtNr,
                    productId = product.id,
                    seriesUuid = product.seriesUUID,
                    articleName = product.articleName,
                    reference = agreement.reference,
                    updatedBy = REGISTER,
                    accessory = product.accessory,
                    sparePart = product.sparePart,
                    isoCategory = product.isoCategory,
                )
            }

        val lagrede =
            productAgreementRegistrationService.saveOrUpdateAll(
                validated,
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

        productAgreementRegistrationService.findById(id)?.let {
            if (it.published > LocalDateTime.now()) {
                LOG.info("Product agreement $id is not published yet, performing physical delete")
                productAgreementRegistrationService.physicalDeleteById(id)
            } else {
                LOG.info("Product agreement $id is published, performing logical delete")
                productAgreementRegistrationService.saveAndCreateEvent(
                    it.copy(
                        status = ProductAgreementStatus.DELETED,
                        updated = LocalDateTime.now(),
                        expired = LocalDateTime.now(),
                        updatedBy = REGISTER,
                    ),
                    isUpdate = true,
                )
            }
        } ?: throw BadRequestException("Product agreement $id not found")
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
        ids.forEach { uuid ->
            productAgreementRegistrationService.findById(uuid)?.let {
                if (it.published > LocalDateTime.now()) {
                    LOG.info("Product agreement $uuid is not published yet, performing physical delete")
                    productAgreementRegistrationService.physicalDeleteById(uuid)
                } else {
                    LOG.info("Product agreement $uuid is published, performing logical delete")
                    productAgreementRegistrationService.saveAndCreateEvent(
                        it.copy(
                            status = ProductAgreementStatus.DELETED,
                            updated = LocalDateTime.now(),
                            expired = LocalDateTime.now(),
                            updatedBy = REGISTER,
                        ),
                        isUpdate = true,
                    )
                }
            } ?: throw BadRequestException("Product agreement $uuid not found")
        }
        return HttpResponse.ok(ProductAgreementsDeletedResponse(ids))
    }

    @Transactional
    open suspend fun persistCatalogResult(catalogImportResult: CatalogImportResult, file: CompletedFileUpload,
                             productAgreementResult: ProductAgreementImportResult) {
        LOG.info("Persisting products from excel imported file: ${file.name}")
        catalogImportService.persistCatalogImportResult(catalogImportResult)
        productAgreementImportExcelService.persistProductAgreementFromCatalogImport(productAgreementResult)
    }
}

data class ProductAgreementsDeletedResponse(
    val ids: List<UUID>,
)

data class ProductAgreementDeletedResponse(
    val id: UUID,
)
