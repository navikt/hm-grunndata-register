package no.nav.hm.grunndata.register.product

import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
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
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.batch.ProductExcelExport
import no.nav.hm.grunndata.register.product.batch.ProductExcelImport
import no.nav.hm.grunndata.register.product.batch.ProductRegistrationExcelDTO
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import java.util.UUID

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductRegistrationApiController.API_V1_PRODUCT_REGISTRATIONS)
@Tag(name = "Vendor Product")
class ProductRegistrationApiController(
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val xlExport: ProductExcelExport,
    private val xlImport: ProductExcelImport,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V1_PRODUCT_REGISTRATIONS = "/vendor/api/v1/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationApiController::class.java)
    }

    @Get("/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        authentication: Authentication,
    ): List<ProductRegistrationDTO> =
        productRegistrationService.findBySeriesUUIDAndSupplierId(seriesUUID, authentication.supplierId())
            .sortedBy { it.created }.map { productDTOMapper.toDTO(it) }

    @Get("/{?params*}")
    suspend fun findProducts(
        @QueryValue params: HashMap<String, String>?,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<ProductRegistrationDTO> = productRegistrationService
        .findAll(buildCriteriaSpec(params, authentication.supplierId()), pageable)
        .mapSuspend { productDTOMapper.toDTO(it) }

    private fun buildCriteriaSpec(
        params: HashMap<String, String>?,
        supplierId: UUID,
    ): PredicateSpecification<ProductRegistration>? =
        params?.let {
            where {
                root[ProductRegistration::supplierId] eq supplierId
                if (params.contains("supplierRef")) root[ProductRegistration::supplierRef] eq params["supplierRef"]
                if (params.contains("seriesUUID")) root[ProductRegistration::seriesUUID] eq params["seriesUUID"]
                if (params.contains("hmsArtNr")) root[ProductRegistration::hmsArtNr] eq params["hmsArtNr"]
                if (params.contains("draft")) root[ProductRegistration::draftStatus] eq DraftStatus.valueOf(params["draft"]!!)
                if (params.contains("registrationStatus")) {
                    val statusList: List<RegistrationStatus> =
                        params["registrationStatus"]!!.split(",").map { RegistrationStatus.valueOf(it) }
                    root[ProductRegistration::registrationStatus] inList statusList
                }
            }.and { root, criteriaBuilder ->
                if (params.contains("title")) {
                    criteriaBuilder.like(
                        root[ProductRegistration::title],
                        params["title"],
                    )
                } else {
                    null
                }
            }
        }

    @Get("/v2/{id}")
    suspend fun getProductByIdV2(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> =
        productRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())
            ?.let { HttpResponse.ok(productDTOMapper.toDTOV2(it)) } ?: HttpResponse.notFound()

    @Put("/v2/{id}")
    suspend fun updateProductV2(
        @Body registrationDTO: UpdateProductRegistrationDTO,
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        try {
            val dto =
                productDTOMapper.toDTO(productRegistrationService.updateProduct(registrationDTO, id, authentication))
            return HttpResponse.ok(dto)
        } catch (dataAccessException: DataAccessException) {
            LOG.error("Got exception while updating product", dataAccessException)
            throw BadRequestException(
                dataAccessException.message ?: "Got exception while updating product $id",
            )
        } catch (e: Exception) {
            LOG.error("Got exception while updating product", e)
            throw BadRequestException("Got exception while updating product $id")
        }
    }

    @Post("/draftWithV3/{seriesUUID}")
    suspend fun createDraft(
        @PathVariable seriesUUID: UUID,
        @Body draftVariant: DraftVariantDTO,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> = try {
        val variant = productRegistrationService.createDraft(seriesUUID, draftVariant, authentication)
        HttpResponse.ok(productDTOMapper.toDTO(variant))
    } catch (dataAccessException: DataAccessException) {
        LOG.error("Got exception while updating product", dataAccessException)
        throw BadRequestException(
            dataAccessException.message ?: "Got exception while creating product",
        )
    } catch (e: Exception) {
        LOG.error("Got exception while updating product", e)
        throw BadRequestException("Got exception while creating product")
    }

    @Put("/to-expired/{id}")
    suspend fun setPublishedProductToInactive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        val updated =
            productRegistrationService.updateRegistrationStatus(id, authentication, RegistrationStatus.INACTIVE)
        return HttpResponse.ok(productDTOMapper.toDTO(updated))
    }

    @Put("/to-active/{id}")
    suspend fun setPublishedProductToActive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTO> {
        val updated = productRegistrationService.updateRegistrationStatus(id, authentication, RegistrationStatus.ACTIVE)
        return HttpResponse.ok(productDTOMapper.toDTO(updated))
    }

    @Delete("/delete")
    suspend fun deleteProducts(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        val updated = productRegistrationService.setDeletedStatus(ids, authentication)
        return HttpResponse.ok(updated.map { productDTOMapper.toDTO(it) })
    }

    @Delete("/draft/delete")
    suspend fun deleteDraftVariants(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        productRegistrationService.deleteDraftVariants(ids, authentication)
        return HttpResponse.ok(emptyList())
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
        products.forEach {
            if (it.supplierId != authentication.supplierId()) {
                throw BadRequestException("Unauthorized access to product ${it.id}")
            }
        }
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
        "/excel/export/supplier",
        consumes = ["application/json"],
        produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
    )
    suspend fun createExportForAllSupplierProducts(authentication: Authentication): HttpResponse<StreamedFile> {
        val products = productRegistrationService.findBySupplierId(authentication.supplierId())
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
        "/excel/export/supplier/{seriesId}",
        consumes = ["application/json"],
        produces = ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"],
    )
    suspend fun createExportForSeries(
        @PathVariable seriesId: UUID,
        authentication: Authentication,
    ): HttpResponse<StreamedFile> {
        val products = productRegistrationService.findBySeriesUUIDAndSupplierId(seriesId, authentication.supplierId())
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
        "/excel/import/{seriesId}",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcelForSeries(
        @PathVariable seriesId: UUID,
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDTO>> {
        LOG.info("Importing Excel file ${file.filename} for supplierId ${authentication.supplierId()}")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            validateProductsToBeImported(excelDTOList, seriesId, authentication)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importExcelRegistrations(excelDTOList, authentication)
            val seriesToUpdate = seriesRegistrationService.findById(seriesId)
            requireNotNull(seriesToUpdate)
            // todo: could it ever be needed to change unpublished to draft also?
            if (seriesToUpdate.published != null) {
                seriesRegistrationService.setSeriesToDraftStatus(seriesToUpdate, authentication)
            }

            HttpResponse.ok(products.map { productDTOMapper.toDTO(it) })
        }
    }

    @Post(
        "/excel/import-dryrun/{seriesId}",
        consumes = [MediaType.MULTIPART_FORM_DATA],
        produces = [MediaType.APPLICATION_JSON],
    )
    suspend fun importExcelForSeriesDryrun(
        @PathVariable seriesId: UUID,
        file: CompletedFileUpload,
        authentication: Authentication,
    ): HttpResponse<List<ProductRegistrationDryRunDTO>> {
        LOG.info("Dryrun for import of Excel file ${file.filename} for supplierId ${authentication.supplierId()}")
        return file.inputStream.use { inputStream ->
            val excelDTOList = xlImport.importExcelFileForRegistration(inputStream)
            validateProductsToBeImported(excelDTOList, seriesId, authentication)
            LOG.info("found ${excelDTOList.size} products in Excel file")
            val products = productRegistrationService.importDryRunExcelRegistrations(excelDTOList, authentication)
            HttpResponse.ok(products)
        }
    }

    private suspend fun validateProductsToBeImported(
        dtos: List<ProductRegistrationExcelDTO>,
        seriesId: UUID,
        authentication: Authentication,
    ) {
        val levArtNrUniqueList = dtos.map { it.levartnr }.distinct()
        if (levArtNrUniqueList.size < dtos.size) {
            throw BadRequestException("Det finnes produkter med samme lev-artnr. i filen. Disse må være unike.")
        }

        val seriesUniqueList = dtos.map { it.produktserieid.toUUID() }.distinct()

        if (seriesUniqueList.size > 1) {
            throw BadRequestException(
                "Det finnes produkter tilknyttet ulike produktserier i filen. " +
                        "Det er kun støtte for å importere produkter til en produktserie om gangen",
            )
        }

        if (seriesUniqueList.size == 1 && seriesUniqueList[0] != seriesId) {
            throw BadRequestException("Produktserien i filen er ulik produktserien du importerte for.")
        }

        seriesUniqueList.forEach {
            if (!productRegistrationService.exitsBySeriesUUIDAndSupplierId(it, authentication.supplierId())) {
                throw BadRequestException("ProduktserieId $it finnes ikke for leverandør ${authentication.supplierId()}")
            }
        }
        dtos.forEach {
            if (it.leverandorid.toUUID() != authentication.supplierId()) {
                throw BadRequestException(
                    "Innlogget bruker har ikke rettigheter til leverandørId ${it.leverandorid}",
                )
            }
        }
    }
}

data class DraftVariantDTO(val articleName: String, val supplierRef: String)

fun Authentication.isSupplier(): Boolean = roles.contains(Roles.ROLE_SUPPLIER)