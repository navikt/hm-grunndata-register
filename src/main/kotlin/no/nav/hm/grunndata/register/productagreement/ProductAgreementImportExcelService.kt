package no.nav.hm.grunndata.register.productagreement

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
import no.nav.hm.grunndata.register.catalog.CatalogImportResult
import no.nav.hm.grunndata.register.product.ProductRegistrationService

@Singleton
open class ProductAgreementImportExcelService(
    private val supplierRegistrationService: SupplierRegistrationService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationService: ProductRegistrationService,
    private val productAgreementService: ProductAgreementRegistrationService,
    private val noDelKontraktHandler: NoDelKontraktHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportExcelService::class.java)
        const val EXCEL = "EXCEL"
    }

    @Transactional
    open suspend fun persistProductAgreementFromCatalogImport(productAgreementResult: ProductAgreementRegistrationResult) {
        productAgreementResult.updatedList.forEach {
            updateProductAndProductAgreement(it)
        }

        productAgreementResult.deactivatedList.forEach {
            deactivateProductAgreement(it)
        }

        productAgreementService.saveAll(productAgreementResult.insertedList)
    }

    private suspend fun deactivateProductAgreement(pa: ProductAgreementRegistrationDTO) {
        LOG.info("Excel import deactivating product agreement for agreement ${pa.agreementId}, " +
                "post ${pa.postId} and productId: ${pa.productId}")
        productAgreementService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
            pa.supplierId, pa.supplierRef, pa.agreementId, pa.postId, pa.rank
        )?.let { existing ->

            productAgreementService.update(
                existing.copy(
                    expired = LocalDateTime.now(),
                    updatedBy = EXCEL,
                    status = ProductAgreementStatus.INACTIVE
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
        productAgreementService.findBySupplierIdAndSupplierRefAndAgreementIdAndPostIdAndRank(
            pa.supplierId, pa.supplierRef, pa.agreementId, pa.postId, pa.rank
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
                    expired = pa.expired
                )
            )
            if (pa.accessory || pa.sparePart && pa.productId != null) {
                productRegistrationService.findById(pa.productId!!)?.let { product ->
                    productRegistrationService.update(
                        product.copy(
                            title = pa.title,
                            articleName = pa.articleName ?: ""
                        )
                    )
                }
            }
        }
    }

    suspend fun mapCatalogImport(catalogImportResult: CatalogImportResult, authentication: Authentication?):
            ProductAgreementRegistrationResult {
        val updatedList = catalogImportResult.updatedList.flatMap { it.toProductAgreementDTO(authentication) }
        val insertedList = catalogImportResult.insertedList.flatMap { it.toProductAgreementDTO(authentication) }
        val deactivatedList = catalogImportResult.deactivatedList.flatMap { it.toProductAgreementDTO(authentication) }
        return ProductAgreementRegistrationResult(updatedList, insertedList, deactivatedList)
    }

    private suspend fun CatalogImport.toProductAgreementDTO(
        authentication: Authentication?,
    ): List<ProductAgreementRegistrationDTO> {
        val cleanRef = reference.lowercase().replace("/", "-")
        val agreement = findAgreementByReferenceLike(cleanRef)
        if (agreement.agreementStatus === AgreementStatus.DELETED) {
            throw BadRequestException("Avtale med anbudsnummer ${agreement.reference} er slettet, må den opprettes?")
        }

        val supplierId = parseSupplierName(supplierName)
        val product = productRegistrationService.findBySupplierRefAndSupplierId(supplierRef, supplierId)
        if (!postNr.isNullOrBlank()) {
            val postRanks: List<Pair<String, Int>> = parsedelkontraktNr(postNr)

            return postRanks.map { postRank ->
                LOG.info("Mapping to product agreement for agreement $cleanRef, post ${postRank.first}, rank ${postRank.second}")
                val delkontrakt: DelkontraktRegistrationDTO =
                    agreement.delkontraktList.find { it.delkontraktData.refNr == postRank.first }
                        ?: throw BadRequestException("Delkontrakt ${postRank.first} finnes ikke i avtale $cleanRef, må den opprettes?")
                ProductAgreementRegistrationDTO(
                    hmsArtNr = parseHMSNr(hmsArtNr),
                    agreementId = agreement.id,
                    supplierRef = supplierRef,
                    productId = product?.id,
                    seriesUuid = product?.seriesUUID,
                    title = title,
                    articleName = product?.articleName,
                    reference = reference,
                    post = delkontrakt.delkontraktData.sortNr,
                    rank = postRank.second,
                    postId = delkontrakt.id,
                    supplierId = supplierId,
                    published = agreement.published,
                    expired = agreement.expired,
                    updatedBy = EXCEL,
                    sparePart = sparePart,
                    accessory = accessory,
                    isoCategory = iso,
                    updatedByUser = authentication?.name ?: "system",
                    status = if (agreement.draftStatus == DraftStatus.DONE && agreement.published < LocalDateTime.now() && agreement.expired > LocalDateTime.now()) ProductAgreementStatus.ACTIVE else ProductAgreementStatus.INACTIVE,
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
                    articleName = product?.articleName,
                    reference = reference,
                    post = noDelKonktraktPost.delkontraktData.sortNr,
                    rank = noDelKonktraktPost.delkontraktData.sortNr,
                    postId = noDelKonktraktPost.id,
                    supplierId = supplierId,
                    published = agreement.published,
                    expired = agreement.expired,
                    updatedBy = EXCEL,
                    sparePart = sparePart,
                    accessory = accessory,
                    isoCategory = iso,
                    updatedByUser = authentication?.name ?: "system",
                    status = if (agreement.draftStatus == DraftStatus.DONE && agreement.published < LocalDateTime.now() && agreement.expired > LocalDateTime.now()) ProductAgreementStatus.ACTIVE else ProductAgreementStatus.INACTIVE,
                ),
            )
        }
    }

    suspend fun findAgreementByReferenceLike(reference: String): AgreementRegistrationDTO =
        agreementRegistrationService.findByReferenceLike("%$reference%")
            ?: throw BadRequestException("Avtale $reference finnes ikke, må den opprettes?")


    private fun parseSupplierName(supplierName: String): UUID =
        runBlocking {
            supplierRegistrationService.findNameAndId().find {
                (it.name.lowercase().indexOf(supplierName.lowercase()) > -1)
            }?.id
                ?: throw BadRequestException("Leverandør $supplierName finnes ikke i registeret, sjekk om navnet er riktig skrevet.")
        }

    private fun parsedelkontraktNr(subContractNr: String): List<Pair<String, Int>> {
        try {
            var matchResult = delKontraktRegex.find(subContractNr)
            val mutableList: MutableList<Pair<String, Int>> = mutableListOf()
            if (matchResult != null) {
                while (matchResult != null) {
                    val groupValues = delKontraktRegex.find(subContractNr)?.groupValues
                    val post = groupValues?.get(1) + groupValues?.get(2)?.uppercase()
                    val rank1 = groupValues?.get(3)?.toIntOrNull() ?: 99
                    mutableList.add(Pair(post, rank1))
                    matchResult = matchResult.next()
                }
            } else {
                throw BadRequestException("Klarte ikke å lese delkontrakt nr. $subContractNr")
            }
            return mutableList
        } catch (e: Exception) {
            LOG.error("Klarte ikke å lese post og rangering fra delkontrakt nr. $subContractNr", e)
            throw BadRequestException("Klarte ikke å lese post og rangering fra delkontrakt nr. $subContractNr")
        }
    }
    private fun parseHMSNr(hmsArtNr: String): String = hmsArtNr.substringBefore(".").toInt().toString()

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

data class CatalogImportExcelDTO(
    val rammeavtaleHandling:String, // oebs operation for rammeavtale
    val bestillingsNr: String,
    val hmsArtNr: String,
    val iso: String,
    val title: String,
    val supplierRef: String,
    val reference: String,
    val delkontraktNr: String?,
    val dateFrom: String,
    val dateTo: String,
    val artikkelHandling: String, // oebs operation for artikkel
    val articleType: String,
    val funksjonsendring: String,
    val forChildren: String,
    val supplierName: String,
    val supplierCity: String,
    val mainProduct: Boolean,
    val sparePart: Boolean,
    val accessory: Boolean,
)

data class ExcelImportedResult(
    val productExcelList: List<CatalogImportExcelDTO>,
    val productAgreementRegistrationList: List<ProductAgreementRegistrationDTO>
)

fun CatalogImportExcelDTO.toEntity() = CatalogImport(
    agreementAction = rammeavtaleHandling,
    orderRef = bestillingsNr,
    hmsArtNr = hmsArtNr,
    iso = iso,
    title = title,
    supplierRef = supplierRef,
    reference = reference,
    postNr = delkontraktNr,
    dateFrom = LocalDate.parse(dateFrom, dateTimeFormatter),
    dateTo = LocalDate.parse(dateTo, dateTimeFormatter),
    articleAction = artikkelHandling,
    articleType = articleType,
    functionalChange = funksjonsendring,
    forChildren = forChildren,
    supplierName = supplierName,
    supplierCity = supplierCity,
    mainProduct = mainProduct,
    sparePart = sparePart,
    accessory = accessory,
)

data class ProductAgreementRegistrationResult(
    val updatedList: List<ProductAgreementRegistrationDTO>,
    val insertedList: List<ProductAgreementRegistrationDTO>,
    val deactivatedList: List<ProductAgreementRegistrationDTO>,
)

val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val delKontraktRegex = Regex("d(\\d+)([A-Q-STU-Z]*)r*(\\d*)")
