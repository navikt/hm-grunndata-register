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
import no.nav.hm.grunndata.rapid.dto.CatalogFileStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.catalog.CatalogExcelFileImport
import no.nav.hm.grunndata.register.catalog.CatalogFile
import no.nav.hm.grunndata.register.catalog.CatalogFileRepository
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.userId
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductAgreementAdminController.ADMIN_API_V1_PRODUCT_AGREEMENT)
@Tag(name = "Admin Product Agreement")
open class ProductAgreementAdminController(
    private val catalogExcelFileImport: CatalogExcelFileImport,
    private val productAgreementImportExcelService: ProductAgreementImportExcelService,
    private val productRegistrationService: ProductRegistrationService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productAgreementRegistrationService: ProductAgreementRegistrationService,
    private val supplierRegistrationService: SupplierRegistrationService,
    private val catalogFileRepository: CatalogFileRepository
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
        @QueryValue supplierId: UUID,
        authentication: Authentication,
    ): ProductAgreementImportDTO {
        val supplier = supplierRegistrationService.findById(supplierId)
            ?: throw BadRequestException("Supplier $supplierId not found")
        LOG.info("Importing excel file: ${file.filename}, dryRun: $dryRun by ${authentication.userId()} for supplier ${supplier.name}")

        val importedExcelCatalog =
            file.inputStream.use { input -> catalogExcelFileImport.importExcelFile(input) }

        val productAgreementsImportResult = productAgreementImportExcelService.mapToProductAgreementImportResult(
            importedExcelCatalog,
            authentication,
            supplierId,
            true, // this has to be always set to true here, cause the persist function will be handled downstream
            false
        )

        LOG.info("New product agreements found: ${productAgreementsImportResult.insertList.size}")
        LOG.info("New series found: ${productAgreementsImportResult.newSeries.size}")
        LOG.info("New accessory parts found: ${productAgreementsImportResult.newAccessoryParts.size}")
        LOG.info("New main products found: ${productAgreementsImportResult.newProducts.size}")

        if (!dryRun) {
            LOG.info("Save the catalog file ${file.filename}, for downstream processing")
            catalogFileRepository.save(
                CatalogFile(
                    fileName = file.filename,
                    fileSize = file.size,
                    orderRef = importedExcelCatalog[0].bestillingsNr,
                    catalogList = importedExcelCatalog,
                    supplierId = supplierId,
                    created = LocalDateTime.now(),
                    updatedByUser = authentication.name,
                    updated = LocalDateTime.now(),
                    status = CatalogFileStatus.PENDING
                )
            )
        }
        val productAgreements = productAgreementsImportResult.insertList
        val newCount = productAgreementsImportResult.insertList.size
        return ProductAgreementImportDTO(
            dryRun = dryRun,
            count = productAgreements.size,
            newCount = newCount,
            file = file.filename,
            supplier = supplier.name,
            createdSeries = productAgreementsImportResult.newSeries,
            createdAccessoryParts = productAgreementsImportResult.newAccessoryParts,
            createdMainProducts = productAgreementsImportResult.newProducts,
            newProductAgreements = productAgreementsImportResult.insertList,
            updatedAgreements = productAgreementsImportResult.updateList,
            deactivatedAgreements = productAgreementsImportResult.deactivateList,
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
        @QueryValue mainProductsOnly: Boolean? = true,
        authentication: Authentication,
    ): List<ProductVariantsForDelkontraktDto> {
        LOG.info("Getting product variants for delkontrakt {$id} by ${authentication.userId()}")
        return productAgreementRegistrationService.findGroupedProductVariantsByDelkontraktId(
            id,
            mainProductsOnly ?: true
        )
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
        productAgreementRegistrationService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
            regDTO.supplierId,
            regDTO.supplierRef,
            regDTO.agreementId,
            regDTO.postId!!
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
                hmsArtNr = product.hmsArtNr!!,
                productId = product.id,
                seriesUuid = product.seriesUUID,
                articleName = product.articleName,
                title = regDTO.title,
                reference = agreement.reference,
                updatedBy = REGISTER,
                accessory = product.accessory,
                sparePart = product.sparePart,
                mainProduct = product.mainProduct,
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
                productAgreementRegistrationService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
                    regDTO.supplierId,
                    regDTO.supplierRef,
                    regDTO.agreementId,
                    regDTO.postId!!
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
                    hmsArtNr = product.hmsArtNr!!,
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
                    LOG.info("Product agreement $uuid is published, deactivating")
                    productAgreementRegistrationService.saveAndCreateEvent(
                        it.copy(
                            status = ProductAgreementStatus.INACTIVE,
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

    @Put(
        value = "/ids",
        consumes = [io.micronaut.http.MediaType.APPLICATION_JSON],
        produces = [io.micronaut.http.MediaType.APPLICATION_JSON],
    )
    suspend fun reactivateProductAgreementByIds(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<ProductAgreementsReactivatedResponse> {
        LOG.info("reactivating product agreements: $ids by ${authentication.userId()}")

        val firstProductAgreement = productAgreementRegistrationService.findById(ids.first())
            ?: throw BadRequestException("Product agreement not found")
        val agreement = agreementRegistrationService.findById(firstProductAgreement.agreementId)
            ?: throw BadRequestException("Agreement not found for product agreement: ${firstProductAgreement.agreementId}")

        ids.forEach { uuid ->
            productAgreementRegistrationService.findById(uuid)?.let {
                LOG.info("Reactivating product agreement wit id $uuid")
                productAgreementRegistrationService.saveAndCreateEvent(
                    it.copy(
                        status = ProductAgreementStatus.ACTIVE,
                        updated = LocalDateTime.now(),
                        expired = agreement.expired,
                        updatedBy = REGISTER,
                    ),
                    isUpdate = true,
                )

            } ?: throw BadRequestException("Product agreement $uuid not found")
        }
        return HttpResponse.ok(ProductAgreementsReactivatedResponse(ids))
    }


}

data class ProductAgreementsDeletedResponse(
    val ids: List<UUID>,
)

data class ProductAgreementsReactivatedResponse(
    val ids: List<UUID>,
)


data class ProductAgreementDeletedResponse(
    val id: UUID,
)
