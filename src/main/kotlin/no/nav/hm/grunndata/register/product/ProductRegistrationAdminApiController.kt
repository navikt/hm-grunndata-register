package no.nav.hm.grunndata.register.product

import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Slice
import io.micronaut.data.model.jpa.criteria.impl.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.data.runtime.criteria.where
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.batch.ProductExcelExport
import no.nav.hm.grunndata.register.product.batch.ProductExcelImport
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesGroupDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

@Secured(Roles.ROLE_ADMIN)
@Controller(ProductRegistrationAdminApiController.API_V1_ADMIN_PRODUCT_REGISTRATIONS)
@Tag(name = "Admin Product")
class ProductRegistrationAdminApiController(
    private val productRegistrationService: ProductRegistrationService,
    private val supplierRegistrationService: SupplierRegistrationService,
    private val xlImport: ProductExcelImport,
    private val xlExport: ProductExcelExport,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V1_ADMIN_PRODUCT_REGISTRATIONS = "/admin/api/v1/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationAdminApiController::class.java)
    }

    @Get("/series/group{?params*}")
    suspend fun findSeriesGroup(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
    ): Slice<SeriesGroupDTO> = productRegistrationService.findSeriesGroup(pageable)

    @Get("/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierId(seriesUUID: UUID) =
        productRegistrationService.findAllBySeriesUuid(seriesUUID).sortedBy { it.created }

    @Get("/{?params*}")
    suspend fun findProducts(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
    ): Page<ProductRegistrationDTO> = productRegistrationService.findAll(buildCriteriaSpec(params), pageable)

    @Get("/til-godkjenning")
    suspend fun findProductsPendingApprove(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
    ): Page<ProductToApproveDto> = productRegistrationService.findProductsToApprove(pageable)

    private fun buildCriteriaSpec(params: HashMap<String, String>?): PredicateSpecification<ProductRegistration>? =
        params?.let {
            where {
                if (params.contains("supplierRef")) root[ProductRegistration::supplierRef] eq params["supplierRef"]
                if (params.contains("hmsArtNr")) root[ProductRegistration::hmsArtNr] eq params["hmsArtNr"]
                if (params.contains("adminStatus")) root[ProductRegistration::adminStatus] eq AdminStatus.valueOf(params["adminStatus"]!!)
                if (params.contains("registrationStatus")) {
                    root[ProductRegistration::registrationStatus] eq
                            RegistrationStatus.valueOf(
                                params["registrationStatus"]!!,
                            )
                }
                if (params.contains("supplierId")) root[ProductRegistration::supplierId] eq UUID.fromString(params["supplierId"]!!)
                if (params.contains("draft")) root[ProductRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
                if (params.contains("createdByUser")) root[ProductRegistration::createdByUser] eq params["createdByUser"]
                if (params.contains("updatedByUser")) root[ProductRegistration::updatedByUser] eq params["updatedByUser"]
            }.and { root, criteriaBuilder ->
                if (params.contains("title")) {
                    criteriaBuilder.like(root[ProductRegistration::title], LiteralExpression("%${params["title"]}%"))
                } else {
                    null
                }
            }
        }

    @Get("/{id}")
    suspend fun getProductById(id: UUID): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(productDTOMapper.toDTO(it))
            }
            ?: HttpResponse.notFound()

    @Get("/v2/{id}")
    suspend fun getProductByIdV2(id: UUID): HttpResponse<ProductRegistrationDTOV2> =
        productRegistrationService.findById(id)
            ?.let {
                HttpResponse.ok(productDTOMapper.toDTOV2(it))
            }
            ?: HttpResponse.notFound()

    @Get("/hmsArtNr/{hmsArtNr}")
    suspend fun getProductByHmsArtNr(hmsArtNr: String): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findByHmsArtNr(hmsArtNr)
            ?.let {
                HttpResponse.ok(productDTOMapper.toDTO(it))
            }
            ?: HttpResponse.notFound()

    @Post("/draftWithV2/{seriesUUID}/supplierId/{supplierId}")
    suspend fun draftProductWithV2(
        @PathVariable seriesUUID: UUID,
        @PathVariable supplierId: UUID,
        @Body draftWith: DraftVariantDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> =
        try {
            HttpResponse.ok(
                productDTOMapper.toDTO(
                    productRegistrationService.createDraftWithV2(
                        seriesUUID,
                        draftWith,
                        authentication
                    )
                ),
            )
        } catch (e: DataAccessException) {
            throw BadRequestException(e.message ?: "Error creating draft")
        } catch (e: Exception) {
            throw BadRequestException("Error creating draft")
        }

    @Post("/draftWithV3/{seriesUUID}")
    suspend fun draftProductWithV3(
        @PathVariable seriesUUID: UUID,
        @Body draftWith: DraftVariantDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> =
        try {
            HttpResponse.ok(
                productDTOMapper.toDTO(
                    productRegistrationService.createDraftWithV2(
                        seriesUUID,
                        draftWith,
                        authentication
                    )
                ),
            )
        } catch (e: DataAccessException) {
            throw BadRequestException(e.message ?: "Error creating draft")
        } catch (e: Exception) {
            throw BadRequestException("Error creating draft")
        }

    @Post("/draft/supplier/{supplierId}{?isAccessory}{?isSparePart}")
    suspend fun draftProduct(
        supplierId: UUID,
        authentication: Authentication,
        @QueryValue(defaultValue = "false") isAccessory: Boolean,
        @QueryValue(defaultValue = "false") isSparePart: Boolean,
    ): HttpResponse<ProductRegistrationDTO> =
        supplierRegistrationService.findById(supplierId)?.let {
            try {
                HttpResponse.ok(
                    productDTOMapper.toDTO(
                        productRegistrationService.createDraft(
                            supplierId,
                            authentication,
                            isAccessory,
                            isSparePart,
                        )
                    ),
                )
            } catch (e: DataAccessException) {
                throw BadRequestException(e.message ?: "Error creating draft")
            } catch (e: Exception) {
                throw BadRequestException("Error creating draft")
            }
        } ?: throw BadRequestException("$supplierId does not exist")

    @Post("/draftWith/supplier/{supplierId}{?isAccessory}{?isSparePart}")
    suspend fun draftProductWith(
        supplierId: UUID,
        @QueryValue(defaultValue = "false") isAccessory: Boolean,
        @QueryValue(defaultValue = "false") isSparePart: Boolean,
        @Body draftWith: ProductDraftWithDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> =
        supplierRegistrationService.findById(supplierId)?.let {
            try {
                HttpResponse.ok(
                    productDTOMapper.toDTO(
                        productRegistrationService.createDraftWith(
                            supplierId,
                            authentication,
                            isAccessory,
                            isSparePart,
                            draftWith,
                        )
                    ),
                )
            } catch (e: DataAccessException) {
                throw BadRequestException(e.message ?: "Error creating draft")
            } catch (e: Exception) {
                throw BadRequestException("Error creating draft")
            }
        } ?: throw BadRequestException("$supplierId does not exist")

    @Post("/")
    suspend fun createProduct(
        @Body registrationDTO: ProductRegistrationDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(registrationDTO.id)?.let {
            throw BadRequestException("Product registration already exists ${registrationDTO.id}")
        } ?: run {
            try {
                val dto =
                    productDTOMapper.toDTO(
                        productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                            registrationDTO
                                .copy(
                                    createdByUser = authentication.name,
                                    updatedByUser = authentication.name,
                                    createdByAdmin = true,
                                    created = LocalDateTime.now(),
                                    updated = LocalDateTime.now(),
                                ).toEntity(),
                            isUpdate = false,
                        )
                    )
                HttpResponse.created(dto)
            } catch (e: DataAccessException) {
                throw BadRequestException(e.message ?: "Error creating product")
            } catch (e: Exception) {
                throw BadRequestException("Error creating product")
            }
        }

    @Put("/v2/{id}")
    suspend fun updateProductV2(
        @Body registrationDTO: UpdateProductRegistrationDTO,
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> =
        try {
            val dto = productDTOMapper.toDTO(
                productRegistrationService.updateProduct(
                    registrationDTO,
                    id,
                    authentication
                )
            )
            HttpResponse.ok(dto)
        } catch (dataAccessException: DataAccessException) {
            LOG.error("Got exception while updating product", dataAccessException)
            throw BadRequestException(
                dataAccessException.message ?: "Got exception while updating product $id",
            )
        } catch (e: Exception) {
            LOG.error("Got exception while updating product", e)
            throw BadRequestException("Got exception while updating product $id")
        }

    @Put("/to-expired/{id}")
    suspend fun setPublishedProductToInactive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        val productToUpdate = productRegistrationService.findById(id) ?: return HttpResponse.notFound()

        val updatedProduct =
            productToUpdate.copy(
                registrationStatus = RegistrationStatus.INACTIVE,
                expired = LocalDateTime.now(),
                updated = LocalDateTime.now(),
                updatedBy = REGISTER,
                updatedByUser = authentication.name,
            )

        val updated =
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)

        return HttpResponse.ok(productDTOMapper.toDTO(updated))
    }

    @Put("/to-active/{id}")
    suspend fun setPublishedProductToActive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        val productToUpdate = productRegistrationService.findById(id) ?: return HttpResponse.notFound()

        val updatedProduct =
            productToUpdate.copy(
                registrationStatus = RegistrationStatus.ACTIVE,
                expired = LocalDateTime.now().plusYears(10),
                updated = LocalDateTime.now(),
                updatedBy = REGISTER,
                updatedByUser = authentication.name,
            )

        val updated =
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(updatedProduct, isUpdate = true)

        return HttpResponse.ok(productDTOMapper.toDTO(updated))
    }

    @Delete("/{id}")
    suspend fun deleteProduct(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> =
        productRegistrationService.findById(id)
            ?.let {
                LOG.info("Deleting product ${it.id}")
                val dto =
                    productDTOMapper.toDTO(
                        productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                            it.copy(
                                registrationStatus = RegistrationStatus.DELETED,
                                expired = LocalDateTime.now(),
                                updatedByUser = authentication.name,
                                updatedBy = REGISTER,
                            ),
                            isUpdate = true,
                        )
                    )
                HttpResponse.ok(dto)
            }
            ?: HttpResponse.notFound()

    @Delete("/delete")
    suspend fun deleteProducts(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        val productsToDelete =
            productRegistrationService.findByIdIn(ids).map {
                it.copy(
                    registrationStatus = RegistrationStatus.DELETED,
                    expired = LocalDateTime.now(),
                    updatedByUser = authentication.name,
                    updatedBy = REGISTER,
                )
            }

        val updated =
            productRegistrationService.saveAllAndCreateEventIfNotDraftAndApproved(productsToDelete, isUpdate = true)

        return HttpResponse.ok(updated.map { productDTOMapper.toDTO(it) })
    }

    @Delete("/draft/delete")
    suspend fun deleteDraftVariants(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        val products =
            productRegistrationService.findByIdIn(ids).onEach {
                if (!(it.draftStatus == DraftStatus.DRAFT && it.published == null)) throw BadRequestException("product is not draft")
            }

        products.forEach {
            LOG.info("Delete called for id ${it.id} and supplierRef ${it.supplierRef} by admin")
        }

        productRegistrationService.deleteAll(products)

        return HttpResponse.ok(emptyList())
    }

    @Post("/draft/variant/{id}")
    suspend fun createProductVariant(
        @PathVariable id: UUID,
        @Body draftVariant: DraftVariantDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        return try {
            productRegistrationService.createProductVariant(id, draftVariant, authentication)?.let {
                HttpResponse.ok(productDTOMapper.toDTO(it))
            } ?: HttpResponse.notFound()
        } catch (e: DataAccessException) {
            LOG.error(
                "Got exception while creating variant ${draftVariant.supplierRef}",
                e,
            )
            throw BadRequestException(e.message ?: "Error creating variant")
        } catch (e: Exception) {
            LOG.error(
                "Got exception while creating variant ${draftVariant.supplierRef}",
                e,
            )
            throw e
        }
    }

    @Put("/reject")
    suspend fun rejectProducts(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        val productsToUpdate =
            productRegistrationService.findByIdIn(ids).onEach {
                if (it.adminStatus != AdminStatus.PENDING) throw BadRequestException("product is not pending approval")
                if (it.draftStatus != DraftStatus.DONE) throw BadRequestException("product is not done")
                if (it.registrationStatus == RegistrationStatus.DELETED) {
                    throw BadRequestException(
                        "RegistrationStatus should not be be Deleted",
                    )
                }
            }

        val productsToBeRejected =
            productsToUpdate.map {
                it.copy(
                    draftStatus = DraftStatus.DRAFT,
                    adminStatus = AdminStatus.REJECTED,
                    updated = LocalDateTime.now(),
                    updatedBy = REGISTER,
                )
            }

        val updated =
            productRegistrationService.saveAllAndCreateEventIfNotDraftAndApproved(productsToBeRejected, isUpdate = true)

        return HttpResponse.ok(updated.map { productDTOMapper.toDTO(it) })
    }

    @Post(
        "/excel/export",
        consumes = ["application/json"],
        produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
    )
    suspend fun createExport(
        @Body uuids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<StreamedFile> {
        val products = uuids.map { productRegistrationService.findById(it) }.filterNotNull()
        if (products.isEmpty()) throw BadRequestException("No products found")
        val id = UUID.randomUUID()
        LOG.info("Generating Excel file: $id.xlsx")
        return ByteArrayOutputStream().use { byteStream ->
            xlExport.createWorkbookToOutputStream(products.map { productDTOMapper.toDTO(it) }, byteStream)
            HttpResponse.ok(StreamedFile(byteStream.toInputStream(), MediaType.MICROSOFT_EXCEL_OPEN_XML_TYPE))
                .header("Content-Disposition", "attachment; filename=$id.xlsx")
        }
    }

    @Post(
        "/excel/import",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcel(
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        LOG.info("Importing Excel file ${file.filename} by admin")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importExcelRegistrations(excelDTOList, authentication)
            HttpResponse.ok(products.map { productDTOMapper.toDTO(it) })
        }
    }

    @Post(
        "/excel/import-dryrun",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcelDryrun(
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDryRunDTO>> {
        LOG.info("Dryrun - Importing Excel file ${file.filename} by admin")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importDryRunExcelRegistrations(excelDTOList, authentication)
            HttpResponse.ok(products)
        }
    }
}

fun Authentication.isAdmin(): Boolean = roles.contains(Roles.ROLE_ADMIN)
