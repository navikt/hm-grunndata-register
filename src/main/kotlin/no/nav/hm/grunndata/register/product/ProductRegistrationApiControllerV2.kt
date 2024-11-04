package no.nav.hm.grunndata.register.product

import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.model.jpa.criteria.impl.expression.LiteralExpression
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import io.micronaut.data.runtime.criteria.get
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.RequestBean
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.batch.ProductExcelImport
import no.nav.hm.grunndata.register.product.batch.ProductRegistrationExcelDTO
import no.nav.hm.grunndata.register.runtime.where
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.security.supplierId
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import org.slf4j.LoggerFactory

@Secured(Roles.ROLE_SUPPLIER)
@Controller(ProductRegistrationApiControllerV2.API_V2_PRODUCT_REGISTRATIONS)
@Tag(name = "Vendor Product")
class ProductRegistrationApiControllerV2(
    private val productRegistrationService: ProductRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val xlImport: ProductExcelImport,
    private val productDTOMapper: ProductDTOMapper,
) {
    companion object {
        const val API_V2_PRODUCT_REGISTRATIONS = "/vendor/api/v2/product/registrations"
        private val LOG = LoggerFactory.getLogger(ProductRegistrationApiControllerV2::class.java)
    }

    @Get("/series/{seriesUUID}")
    suspend fun findBySeriesUUIDAndSupplierId(
        seriesUUID: UUID,
        authentication: Authentication,
    ): List<ProductRegistrationDTOV2> =
        productRegistrationService.findBySeriesUUIDAndSupplierId(seriesUUID, authentication.supplierId())
            .sortedBy { it.created }.map { productDTOMapper.toDTOV2(it) }

    @Get("/")
    suspend fun findProducts(
        @RequestBean criteria: ProductRegistrationCriteria,
        pageable: Pageable,
        authentication: Authentication,
    ): Page<ProductRegistrationDTOV2> = productRegistrationService
        .findAll(buildCriteriaSpec(criteria, authentication.supplierId()), pageable)
        .mapSuspend { productDTOMapper.toDTOV2(it) }

    private fun buildCriteriaSpec(
        criteria: ProductRegistrationCriteria,
        supplierId: UUID,
    ): PredicateSpecification<ProductRegistration>? =
        if (criteria.isNotEmpty()) {
            where {
                root[ProductRegistration::supplierId] eq supplierId
                criteria.supplierRef?.let { root[ProductRegistration::supplierRef] eq it }
                criteria.seriesUUID?.let { root[ProductRegistration::seriesUUID] eq it }
                criteria.hmsArtNr?.let { root[ProductRegistration::hmsArtNr] eq it }
                criteria.draft?.let { root[ProductRegistration::draftStatus] eq it }
                criteria.registrationStatus?.let { statusList ->
                    root[ProductRegistration::registrationStatus] inList statusList
                }
                criteria.title?.let { root[ProductRegistration::title] like LiteralExpression("%$it%") }
            }
        } else null

    @Get("/{id}")
    suspend fun getProductById(
        id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> =
        productRegistrationService.findByIdAndSupplierId(id, authentication.supplierId())
            ?.let { HttpResponse.ok(productDTOMapper.toDTOV2(it)) } ?: HttpResponse.notFound()

    @Put("/{id}")
    suspend fun updateProduct(
        @Body registrationDTO: UpdateProductRegistrationDTO,
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<ProductRegistrationDTOV2> {
        try {
            val dto =
                productDTOMapper.toDTOV2(productRegistrationService.updateProduct(registrationDTO, id, authentication))
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
    ): HttpResponse<ProductRegistrationDTOV2> = try {
        val variant = productRegistrationService.createDraft(seriesUUID, draftVariant, authentication)
        HttpResponse.ok(productDTOMapper.toDTOV2(variant))
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
    ): HttpResponse<Any> {
        productRegistrationService.updateRegistrationStatus(id, authentication, RegistrationStatus.INACTIVE)
        return HttpResponse.ok()
    }

    @Put("/to-active/{id}")
    suspend fun setPublishedProductToActive(
        @PathVariable id: UUID,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.updateRegistrationStatus(id, authentication, RegistrationStatus.ACTIVE)
        return HttpResponse.ok()
    }

    @Delete("/delete")
    suspend fun deleteProducts(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.setDeletedStatus(ids, authentication)
        return HttpResponse.ok()
    }

    @Delete("/draft/delete")
    suspend fun deleteDraftVariants(
        @Body ids: List<UUID>,
        authentication: Authentication,
    ): HttpResponse<Any> {
        productRegistrationService.deleteDraftVariants(ids, authentication)
        return HttpResponse.ok()
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
    ): HttpResponse<List<ProductRegistrationDTOV2>> {
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

            HttpResponse.ok(products.map { productDTOMapper.toDTOV2(it) })
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