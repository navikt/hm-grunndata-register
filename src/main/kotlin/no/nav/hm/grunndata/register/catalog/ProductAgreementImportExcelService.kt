package no.nav.hm.grunndata.register.catalog

import io.micronaut.security.authentication.Authentication
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
import no.nav.hm.grunndata.register.agreement.NoDelKontraktHandler
import no.nav.hm.grunndata.register.error.BadRequestException
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationService

@Singleton
open class ProductAgreementImportExcelService(
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val productAgreementService: no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService,
    private val noDelKontraktHandler: NoDelKontraktHandler,
    private val catalogImportService: CatalogImportService,
    private val productAccessorySparePartAgreementHandler: ProductAccessorySparePartAgreementHandler,
    private val seriesRegistrationService: SeriesRegistrationService
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportExcelService::class.java)
        const val EXCEL = "EXCEL"
    }

    @Transactional
    open suspend fun persistProductAgreementFromCatalogImport(productAgreementResult: ProductAgreementImportResult) {
        val updated = productAgreementResult.updateList.map {
            updateProductAndProductAgreement(it)
        }

        val deactivated = productAgreementResult.deactivateList.map {
            deactivateProductAgreement(it)
        }

        val inserted = productAgreementResult.insertList.map {
            updateProductAndProductAgreement(it)
        }
        val distinct = (updated + deactivated + inserted).distinctBy { it.productId }
        // ssave and create events for all products
        distinct.forEach {
            if (it.mainProduct) {
                if (it.productId == null) {
                    throw BadRequestException(
                        "Product agreement with hmsnr: ${it.hmsArtNr} with main product ${it.title} has no productId, cannot save, " +
                                "check catalog import table, product probably deleted!"
                    )
                }
                productRegistrationService.findById(it.productId)?.let { product ->
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product, isUpdate = true)
                }
            } else {
                updateAccessoryAndSparePart(it)
            }
        }
    }

    private suspend fun deactivateProductAgreement(pa: no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO): no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO {
        LOG.info(
            "Excel import deactivating product agreement for agreement ${pa.agreementId}, " +
                    "post ${pa.postId} and productId: ${pa.productId}"
        )
        return productAgreementService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
            pa.supplierId, pa.supplierRef, pa.agreementId, pa.postId
        )?.let { existing ->

            productAgreementService.update(
                existing.copy(
                    expired = LocalDateTime.now(),
                    updatedBy = EXCEL,
                    status = ProductAgreementStatus.INACTIVE,
                    updated = LocalDateTime.now()
                )
            )
        } ?: pa
    }


    private suspend fun updateProductAndProductAgreement(pa: no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO): no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO =
        productAgreementService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
            pa.supplierId, pa.supplierRef, pa.agreementId, pa.postId
        )?.let { existing ->
            LOG.info(
                "Excel import updating product agreement for agreement ${pa.agreementId}, " +
                        "post ${pa.postId} and productId: ${pa.productId}"
            )
            productAgreementService.update(
                existing.copy(
                    productId = pa.productId,
                    hmsArtNr = pa.hmsArtNr,
                    seriesUuid = pa.seriesUuid,
                    title = pa.title,
                    articleName = pa.articleName,
                    sparePart = pa.sparePart,
                    accessory = pa.accessory,
                    updatedBy = EXCEL,
                    published = pa.published,
                    expired = pa.expired,
                    updated = LocalDateTime.now(),
                    status = pa.status
                )
            )
        } ?: productAgreementService.save(pa)

    open suspend fun updateAccessoryAndSparePart(
        pa: no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO,
    ) {
        productRegistrationService.findById(pa.productId!!)?.let { product ->
            val series = seriesRegistrationService.findById(product.seriesUUID)
            seriesRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                series!!.copy(
                    title = pa.articleName ?: pa.title
                ), true
            )
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                product.copy(
                    title = pa.title,
                    articleName = pa.articleName ?: "",
                    updated = LocalDateTime.now(),
                    draftStatus = DraftStatus.DONE,
                    adminStatus = AdminStatus.APPROVED,
                    mainProduct = pa.mainProduct,
                    accessory = pa.accessory,
                    sparePart = pa.sparePart,
                    published = pa.published,
                    expired = pa.expired,
                    registrationStatus = if (pa.status == ProductAgreementStatus.ACTIVE) RegistrationStatus.ACTIVE else RegistrationStatus.INACTIVE,
                ), true
            )
        }
    }

    suspend fun mapToProductAgreementImportResult(
        importedExcelCatalog: List<CatalogImportExcelDTO>,
        authentication: Authentication?,
        supplierId: UUID,
        dryRun: Boolean,
        forceUpdate: Boolean,
    ): ProductAgreementImportResult {
        verifyCatalogImportList(importedExcelCatalog)
        val agreementRef = importedExcelCatalog.first().reference
        val cleanRef = agreementRef.lowercase().replace("/", "-")
        val agreement = agreementRegistrationService.findByReferenceLike("%$cleanRef%")
            ?: throw IllegalArgumentException("Agreement reference: $cleanRef not found!")
        if (agreement.agreementStatus === AgreementStatus.DELETED) {
            throw BadRequestException("Avtale med anbudsnummer ${agreement.reference} er slettet, må den opprettes?")
        }
        val catalogImportResult = catalogImportService.prepareCatalogImportResult(importedExcelCatalog.map {
            it.toEntity(
                agreement,
                supplierId
            )
        }, forceUpdate)
        val mappedLists = mapCatalogImport(catalogImportResult, authentication, supplierId, dryRun)
        val productAgreementImportResult = productAccessorySparePartAgreementHandler.handleNewProductsInExcelImport(
            mappedLists,
            authentication,
            dryRun
        )
        if (!dryRun) {
            LOG.info("Persisting products and agreements from excel import")
            persistCatalogResult(catalogImportResult, productAgreementImportResult)
        }
        LOG.info(
            "Excel import for orderRef: ${importedExcelCatalog.first().bestillingsNr} with supplier ${importedExcelCatalog.first().supplierName} " +
                    "resulted in ${productAgreementImportResult.insertList.size} inserted, " +
                    "${productAgreementImportResult.updateList.size} updated and ${productAgreementImportResult.deactivateList.size} deactivated product agreements"
        )
        return productAgreementImportResult
    }


    private fun verifyCatalogImportList(catalogImportList: List<CatalogImportExcelDTO>) {
        if (catalogImportList.isEmpty()) {
            throw IllegalArgumentException("Catalog import list is empty")
        }
        if (catalogImportList.map { it.bestillingsNr }.distinct().size > 1) {
            throw IllegalArgumentException("Ugyldig katalog, inneholder flere bestillingsnr")
        }
        if (catalogImportList.map { it.supplierName }.distinct().size > 1) {
            throw IllegalArgumentException("Ugyldig katalog, inneholder flere leverandører")
        }
        if (catalogImportList.map { it.reference }.distinct().size > 1) {
            throw IllegalArgumentException("Ugylding katalog, inneholder flere rammeavtale referansenr")
        }
    }

    @Transactional
    open suspend fun persistCatalogResult(
        catalogImportResult: CatalogImportResult,
        productAgreementResult: ProductAgreementImportResult
    ) {
        catalogImportService.persistCatalogImportResult(catalogImportResult)
        persistProductAgreementFromCatalogImport(productAgreementResult)
    }

    suspend fun mapCatalogImport(
        catalogImportResult: CatalogImportResult,
        authentication: Authentication?,
        supplierId: UUID,
        dryRun: Boolean
    ):
            ProductAgreementMappedResultLists {
        val updatedList =
            catalogImportResult.updatedList.flatMap { it.toProductAgreementDTO(authentication, supplierId, dryRun) }
        val insertedList =
            catalogImportResult.insertedList.flatMap { it.toProductAgreementDTO(authentication, supplierId, dryRun) }
        val deactivatedList =
            catalogImportResult.deactivatedList.flatMap { it.toProductAgreementDTO(authentication, supplierId, dryRun) }
        return ProductAgreementMappedResultLists(updatedList, insertedList, deactivatedList)
    }

    private suspend fun CatalogImport.toProductAgreementDTO(
        authentication: Authentication?, supplierId: UUID, dryRun: Boolean
    ): List<no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO> {
        val agreement = agreementRegistrationService.findById(agreementId)
            ?: throw BadRequestException("Avtale med id $agreementId finnes ikke, må den opprettes?")
        val product = productRegistrationService.findBySupplierRefAndSupplierId(supplierRef, supplierId)
        if (product != null && mainProduct && !product.mainProduct) {
            LOG.warn("Catalog import has main product set to true, but product inDb ${product.id} is not main product")
            if (!dryRun) productRegistrationService.update(product.copy(mainProduct = true))
        }
        if (!postNr.isNullOrBlank()) {
            val postRanks: List<Pair<String, Int>> = parsedelkontraktNr(postNr)

            return postRanks.map { postRank ->
                LOG.info("Mapping to product agreement for agreement ${agreement.reference}, post ${postRank.first}, rank ${postRank.second}")
                val delkontrakt: DelkontraktRegistrationDTO =
                    agreement.delkontraktList.find { it.delkontraktData.refNr == postRank.first }
                        ?: throw BadRequestException("Delkontrakt ${postRank.first} finnes ikke i avtale ${agreement.reference}, må den opprettes?")
                ProductAgreementRegistrationDTO(
                    hmsArtNr = parseHMSNr(hmsArtNr),
                    agreementId = agreement.id,
                    supplierRef = supplierRef,
                    productId = product?.id,
                    seriesUuid = product?.seriesUUID,
                    title = title,
                    articleName = title,
                    reference = reference,
                    post = delkontrakt.delkontraktData.sortNr,
                    rank = postRank.second,
                    postId = delkontrakt.id,
                    supplierId = supplierId,
                    published = dateFrom.atTime(0, 0, 0),
                    expired = dateTo.atTime(23, 59, 59),
                    updatedBy = EXCEL,
                    sparePart = sparePart,
                    accessory = accessory,
                    mainProduct = mainProduct,
                    isoCategory = iso,
                    updatedByUser = authentication?.name ?: "system",
                    status = mapProductAgreementStatus(agreement),
                )
            }
        } else {
            val noDelKonktraktPost = noDelKontraktHandler.findAndCreateWithNoDelkonktraktTypeIfNotExists(agreement.id)

            return listOf(
                ProductAgreementRegistrationDTO(
                    hmsArtNr = parseHMSNr(hmsArtNr),
                    agreementId = agreement.id,
                    supplierRef = supplierRef,
                    productId = product?.id,
                    seriesUuid = product?.seriesUUID,
                    title = title,
                    articleName = product?.articleName ?: title,
                    reference = reference,
                    post = noDelKonktraktPost.delkontraktData.sortNr,
                    rank = noDelKonktraktPost.delkontraktData.sortNr,
                    postId = noDelKonktraktPost.id,
                    supplierId = supplierId,
                    published = dateFrom.atTime(0, 0, 0),
                    expired = dateTo.atTime(23, 59, 59),
                    updatedBy = EXCEL,
                    sparePart = sparePart,
                    accessory = accessory,
                    mainProduct = mainProduct,
                    isoCategory = iso,
                    updatedByUser = authentication?.name ?: "system",
                    status = mapProductAgreementStatus(agreement),
                ),
            )
        }
    }

    private fun CatalogImport.mapProductAgreementStatus(agreement: AgreementRegistrationDTO): ProductAgreementStatus {
        val nowDate = LocalDate.now()
        return if (agreement.draftStatus == DraftStatus.DONE
            && dateFrom <= nowDate
            && dateTo > nowDate
        )
            ProductAgreementStatus.ACTIVE
        else ProductAgreementStatus.INACTIVE
    }


}

fun parseHMSNr(hmsArtNr: String): String {
    val parsedNumber = hmsArtNr.substringBefore(".").toInt().toString()
    return parsedNumber.padStart(6, '0')
}

fun parsedelkontraktNr(subContractNr: String): List<Pair<String, Int>> {
    try {
        val cleanSubContractNr = subContractNr.replace("\\s".toRegex(), "")

        var matchResult = delKontraktRegex.find(cleanSubContractNr)
        val mutableList: MutableList<Pair<String, Int>> = mutableListOf()
        if (matchResult != null) {
            while (matchResult != null) {
                val groupValues = matchResult.groupValues
                val post = groupValues[1] + groupValues[2].uppercase()
                val rank1 = groupValues[3].toIntOrNull() ?: 99
                mutableList.add(Pair(post, rank1))
                matchResult = matchResult.next()
            }
        } else {
            throw BadRequestException("Klarte ikke å lese delkontrakt nr. $subContractNr")
        }
        return mutableList
    } catch (e: Exception) {
        throw BadRequestException("Klarte ikke å lese post og rangering fra delkontrakt nr. $subContractNr")
    }

}

enum class ColumnNames(val column: String) {
    rammeavtaleHandling("RammeavtaleHandling"),
    bestillingsnr("Bestillingsnr"),
    hms_ArtNr("HMS-Artnr"),
    kategori("Kategori"),
    beskrivelse("Beskrivelse"),
    leverandørensartnr("Leverandørensartnr"),
    anbudsnr("Anbudsnr"),
    delkontraktnummer("Delkontraktnummer"),
    datofom("Datofom"),
    datotom("Datotom"),
    artikkelHandling("ArtikkelHandling"),
    malTypeartikkel("MalTypeartikkel"),
    funksjonsendring("Funksjonsendring"),
    malgruppebarn("Målgruppebarn"),
    leverandorfirmanavn("LeverandørFirmanavn"),
    leverandorsted("Leverandørsted"),
}


data class ProductAgreementMappedResultLists(
    val updateList: List<no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO>,
    val insertList: List<no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO>,
    val deactivateList: List<no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO>,
)

val delKontraktRegex = Regex("d(\\d+)([A-Q-STU-Z]*)r*(\\d*),*")
