package no.nav.hm.grunndata.register.productagreement

import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.security.authentication.Authentication
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
import no.nav.hm.grunndata.register.agreement.NoDelKontraktHandler
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.catalog.CatalogImport
import no.nav.hm.grunndata.register.catalog.CatalogImportExcelDTO
import no.nav.hm.grunndata.register.catalog.CatalogImportResult
import no.nav.hm.grunndata.register.catalog.CatalogImportService
import no.nav.hm.grunndata.register.catalog.toEntity
import no.nav.hm.grunndata.register.product.ProductRegistrationService

@Singleton
open class ProductAgreementImportExcelService(
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val productAgreementService: ProductAgreementRegistrationService,
    private val noDelKontraktHandler: NoDelKontraktHandler,
    private val catalogImportService: CatalogImportService,
    private val productAccessorySparePartAgreementHandler: ProductAccessorySparePartAgreementHandler
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportExcelService::class.java)
        const val EXCEL = "EXCEL"
    }

    @Transactional
    open suspend fun persistProductAgreementFromCatalogImport(productAgreementResult: ProductAgreementImportResult) {
        productAgreementResult.updateList.forEach {
            updateProductAndProductAgreement(it)
        }

        productAgreementResult.deactivateList.forEach {
            deactivateProductAgreement(it)
        }

        productAgreementService.saveAll(productAgreementResult.insertList)
    }

    private suspend fun deactivateProductAgreement(pa: ProductAgreementRegistrationDTO) {
        LOG.info("Excel import deactivating product agreement for agreement ${pa.agreementId}, " +
                "post ${pa.postId} and productId: ${pa.productId}")
        productAgreementService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
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
            if (pa.productId != null && (pa.accessory || pa.sparePart)) {
                productRegistrationService.findById(pa.productId)?.let { product ->
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                        product.copy(
                            expired = LocalDateTime.now(),
                            updatedBy = REGISTER,
                            updatedByUser = "EXCEL",
                            registrationStatus = RegistrationStatus.INACTIVE
                        ), true
                    )
                }
            }
        }
    }



    private suspend fun updateProductAndProductAgreement(pa: ProductAgreementRegistrationDTO) {
        productAgreementService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostId(
            pa.supplierId, pa.supplierRef, pa.agreementId, pa.postId
        )?.let { existing ->
            LOG.info("Excel import updating product agreement for agreement ${pa.agreementId}, " +
                    "post ${pa.postId} and productId: ${pa.productId}")
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
                    expired = pa.expired,
                    updated = LocalDateTime.now(),
                    status = pa.status
                )
            )
            if (pa.accessory || pa.sparePart && pa.productId != null) {
                productRegistrationService.findById(pa.productId!!)?.let { product ->
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                        product.copy(
                            title = pa.title,
                            articleName = pa.articleName ?: ""
                        ), true
                    )
                }
            }
        }
    }

    suspend fun mapToProductAgreementImportResult(
        importedExcelCatalog:  List<CatalogImportExcelDTO>,
        authentication: Authentication?,
        supplierId: UUID,
        dryRun: Boolean
    ): ProductAgreementImportResult {
        verifyCatalogImportList(importedExcelCatalog)
        val agreementRef = importedExcelCatalog.first().reference
        val cleanRef = agreementRef.lowercase().replace("/", "-")
        val agreement = agreementRegistrationService.findByReferenceLike("%$cleanRef%") ?: throw IllegalArgumentException("Agreement reference: $cleanRef not found!")
        if (agreement.agreementStatus === AgreementStatus.DELETED) {
            throw BadRequestException("Avtale med anbudsnummer ${agreement.reference} er slettet, må den opprettes?")
        }
        val catalogImportResult = catalogImportService.prepareCatalogImportResult(importedExcelCatalog.map { it.toEntity(agreement) })
        val mappedLists = mapCatalogImport(catalogImportResult, authentication, supplierId)
        val productAgreementImportResult = productAccessorySparePartAgreementHandler.handleNewProductsInExcelImport(mappedLists, authentication, dryRun)
        if (!dryRun) {
            LOG.info("Persisting products and agreements from excel import")
            persistCatalogResult(catalogImportResult, productAgreementImportResult)
        }
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
    open suspend fun persistCatalogResult(catalogImportResult: CatalogImportResult,
                                          productAgreementResult: ProductAgreementImportResult) {
        catalogImportService.persistCatalogImportResult(catalogImportResult)
        persistProductAgreementFromCatalogImport(productAgreementResult)
    }

    suspend fun mapCatalogImport(catalogImportResult: CatalogImportResult, authentication: Authentication?,  supplierId: UUID):
            ProductAgreementMappedResultLists {
        val updatedList = catalogImportResult.updatedList.flatMap { it.toProductAgreementDTO(authentication, supplierId) }
        val insertedList = catalogImportResult.insertedList.flatMap { it.toProductAgreementDTO(authentication, supplierId) }
        val deactivatedList = catalogImportResult.deactivatedList.flatMap { it.toProductAgreementDTO(authentication, supplierId) }
        return ProductAgreementMappedResultLists(updatedList, insertedList, deactivatedList)
    }

    private suspend fun CatalogImport.toProductAgreementDTO(
        authentication: Authentication?, supplierId: UUID
    ): List<ProductAgreementRegistrationDTO> {
        val agreement = agreementRegistrationService.findById(agreementId)
            ?: throw BadRequestException("Avtale med id $agreementId finnes ikke, må den opprettes?")
        val product = productRegistrationService.findBySupplierRefAndSupplierId(supplierRef, supplierId)
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
                    articleName = product?.articleName ?: title,
                    reference = reference,
                    post = delkontrakt.delkontraktData.sortNr,
                    rank = postRank.second,
                    postId = delkontrakt.id,
                    supplierId = supplierId,
                    published = agreement.published,
                    expired = dateTo.atTime(23,59,59),
                    updatedBy = EXCEL,
                    sparePart = sparePart,
                    accessory = accessory,
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
                    published = agreement.published,
                    expired = dateTo.atTime(23,59,59,),
                    updatedBy = EXCEL,
                    sparePart = sparePart,
                    accessory = accessory,
                    isoCategory = iso,
                    updatedByUser = authentication?.name ?: "system",
                    status = mapProductAgreementStatus(agreement),
                ),
            )
        }
    }

    private fun CatalogImport.mapProductAgreementStatus(agreement: AgreementRegistrationDTO): ProductAgreementStatus {
        val nowDate = LocalDate.now()
        val now = LocalDateTime.now()
        return if (agreement.draftStatus == DraftStatus.DONE
                    && agreement.published < now
                    && agreement.expired > now
                    && dateTo > nowDate)
             ProductAgreementStatus.ACTIVE
        else ProductAgreementStatus.INACTIVE
    }

    suspend fun findAgreementByReferenceLike(reference: String): AgreementRegistrationDTO =
        agreementRegistrationService.findByReferenceLike("%$reference%")
            ?: throw BadRequestException("Avtale $reference finnes ikke, må den opprettes?")






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
    val updateList: List<ProductAgreementRegistrationDTO>,
    val insertList: List<ProductAgreementRegistrationDTO>,
    val deactivateList: List<ProductAgreementRegistrationDTO>,
)

val delKontraktRegex = Regex("d(\\d+)([A-Q-STU-Z]*)r*(\\d*),*")
