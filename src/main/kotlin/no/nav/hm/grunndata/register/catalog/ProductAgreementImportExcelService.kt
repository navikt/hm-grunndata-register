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
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationService
import no.nav.hm.grunndata.register.series.SeriesRegistrationService

@Singleton
open class ProductAgreementImportExcelService(
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val productAgreementService: ProductAgreementRegistrationService,
    private val noDelKontraktHandler: NoDelKontraktHandler,
    private val catalogImportService: CatalogImportService,
    private val seriesRegistrationService: SeriesRegistrationService
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportExcelService::class.java)
        const val EXCEL = "EXCEL"
    }

    @Transactional
    open suspend fun persistProductAgreementFromCatalogImport(mappedResultLists: ProductAgreementMappedResultLists) {
        val updated = mappedResultLists.updateList.map {
            updateProductAndProductAgreement(it)
        }

        val deactivated = mappedResultLists.deactivateList.map {
            deactivateProductAgreement(it)
        }

        val inserted = mappedResultLists.insertList.map {
            updateProductAndProductAgreement(it)
        }
        val distinct = (updated + deactivated + inserted).distinctBy { it.productId }
        // ssave and create events for all products
        distinct.forEach {
            if (it.mainProduct) {
                productRegistrationService.findById(it.productId)?.let { product ->
                    productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product, isUpdate = true)
                }
            } else {
                LOG.info("updating product agreement for accessory/sparepart with hmsnr: ${it.hmsArtNr} with title ${it.title} and productId ${it.productId}")
                updateAccessoryAndSparePart(it)
            }
        }
    }

    private suspend fun deactivateProductAgreement(pa: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO {
        LOG.info(
            "Excel import deactivating product agreement for agreement ${pa.agreementId}, " +
                    "post ${pa.postId} and productId: ${pa.productId}"
        )
        return productAgreementService.findByProductIdAndAgreementIdAndPostId(
            pa.productId, pa.agreementId, pa.postId
        )?.let { existing ->
            productAgreementService.update(
                existing.copy(
                    expired = LocalDateTime.now(),
                    updatedBy = EXCEL,
                    hmsArtNr = pa.hmsArtNr,
                    supplierRef = pa.supplierRef,
                    status = ProductAgreementStatus.INACTIVE,
                    updated = LocalDateTime.now()
                )
            )
        } ?: pa
    }

    private suspend fun updateProductAndProductAgreement(pa: ProductAgreementRegistrationDTO): ProductAgreementRegistrationDTO =
        productAgreementService.findByProductIdAndAgreementIdAndPostId(
            pa.productId, pa.agreementId, pa.postId
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
                    supplierRef = pa.supplierRef,
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
        pa: ProductAgreementRegistrationDTO,
    ) {
        productRegistrationService.findById(pa.productId)?.let { product ->
            LOG.info("Excel import updating accessory/sparepart product ${product.id} for product agreement ${pa.agreementId}, post ${pa.postId} and seriesId: ${product.seriesUUID}")
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
                    mainProduct = if (!product.mainProduct) pa.mainProduct else true, // prevent changing mainProduct from true to false
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
        catalogImportResult: CatalogImportResult,
        authentication: Authentication,
        supplierId: UUID
    ): ProductAgreementMappedResultLists {

        val mappedLists = mapCatalogImportToProductAgreement(catalogImportResult, authentication, supplierId)
        LOG.info("Persisting products and agreements from excel import")
        persistCatalogResult(catalogImportResult, mappedLists)

        return mappedLists
    }


    @Transactional
    open suspend fun persistCatalogResult(
        catalogImportResult: CatalogImportResult,
        productAgreementResult: ProductAgreementMappedResultLists
    ) {
        catalogImportService.persistCatalogImportResult(catalogImportResult)
        persistProductAgreementFromCatalogImport(productAgreementResult)
    }

    suspend fun mapCatalogImportToProductAgreement(
        catalogImportResult: CatalogImportResult,
        authentication: Authentication,
        supplierId: UUID
    ): ProductAgreementMappedResultLists {
        val updatedList =
            catalogImportResult.updatedList.flatMap { it.toProductAgreementDTO(authentication, supplierId) }
        val insertedList =
            catalogImportResult.insertedList.flatMap { it.toProductAgreementDTO(authentication, supplierId) }
        val deactivatedList =
            catalogImportResult.deactivatedList.flatMap { it.toProductAgreementDTO(authentication, supplierId) }
        return ProductAgreementMappedResultLists(updatedList, insertedList, deactivatedList)
    }

    private suspend fun CatalogImport.toProductAgreementDTO(
        authentication: Authentication,
        supplierId: UUID
    ): List<ProductAgreementRegistrationDTO> {
        val agreement = agreementRegistrationService.findById(agreementId)
            ?: throw BadRequestException("Avtale med id $agreementId finnes ikke, må den opprettes?")
        val product = productRegistrationService.findByHmsArtNrAndSupplierId(hmsArtNr, supplierId) ?: throw BadRequestException("Produkt med hmsArtNr: $hmsArtNr finnes ikke. Produktet må være opprettet før katalogavtaleimport.")
        if (!postNr.isNullOrBlank()) {
            val postRanks: List<Pair<String, Int>> = parsedelkontraktNr(postNr)

            return postRanks.map { postRank ->
                LOG.info("Mapping to product agreement for agreement ${agreement.reference}, post ${postRank.first}, rank ${postRank.second}")
                val delkontrakt: DelkontraktRegistrationDTO =
                    agreement.delkontraktList.find { it.delkontraktData.refNr == postRank.first }
                        ?: throw BadRequestException("Delkontrakt ${postRank.first} finnes ikke i avtale ${agreement.reference}, må den opprettes?")
                ProductAgreementRegistrationDTO(
                    hmsArtNr = hmsArtNr,
                    agreementId = agreement.id,
                    supplierRef = supplierRef,
                    productId = product.id,
                    seriesUuid = product.seriesUUID,
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
                    updatedByUser = authentication.name,
                    status = mapProductAgreementStatus(agreement),
                )
            }
        } else {
            val noDelKonktraktPost = noDelKontraktHandler.findAndCreateWithNoDelkonktraktTypeIfNotExists(agreement.id)

            return listOf(
                ProductAgreementRegistrationDTO(
                    hmsArtNr = hmsArtNr,
                    agreementId = agreement.id,
                    supplierRef = supplierRef,
                    productId = product.id,
                    seriesUuid = product?.seriesUUID,
                    title = title,
                    articleName = product.articleName,
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
    try {
        val parsedNumber = hmsArtNr.substringBefore(".").toInt().toString()
        return parsedNumber.padStart(6, '0')
    }
    catch (e: Exception) {
        throw IllegalArgumentException("Klarte ikke å lese HMS artnr. $hmsArtNr. Årsak: ${e.message}", e)
    }

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
