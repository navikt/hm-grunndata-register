package no.nav.hm.grunndata.register.productagreement

import io.micronaut.security.authentication.Authentication
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
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.productagreement.ColumnNames.anbudsnr
import no.nav.hm.grunndata.register.productagreement.ColumnNames.beskrivelse
import no.nav.hm.grunndata.register.productagreement.ColumnNames.datofom
import no.nav.hm.grunndata.register.productagreement.ColumnNames.datotom
import no.nav.hm.grunndata.register.productagreement.ColumnNames.delkontraktnummer
import no.nav.hm.grunndata.register.productagreement.ColumnNames.funksjonsendring
import no.nav.hm.grunndata.register.productagreement.ColumnNames.hms_ArtNr
import no.nav.hm.grunndata.register.productagreement.ColumnNames.kategori
import no.nav.hm.grunndata.register.productagreement.ColumnNames.leverandorfirmanavn
import no.nav.hm.grunndata.register.productagreement.ColumnNames.leverandorsted
import no.nav.hm.grunndata.register.productagreement.ColumnNames.leverandørensartnr
import no.nav.hm.grunndata.register.productagreement.ColumnNames.malTypeartikkel
import no.nav.hm.grunndata.register.productagreement.ColumnNames.malgruppebarn
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.apache.poi.openxml4j.util.ZipSecureFile
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.hm.grunndata.register.catalog.CatalogImport

@Singleton
class ProductAgreementImportExcelService(
    private val supplierRegistrationService: SupplierRegistrationService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationRepository: ProductRegistrationRepository,
    private val noDelKontraktHandler: NoDelKontraktHandler,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportExcelService::class.java)
        const val EXCEL = "EXCEL"
    }



    suspend fun mapCatalogImport(catalogImportList: List<CatalogImportExcelDTO>, authentication: Authentication?): List<ProductAgreementRegistrationDTO> {
        return catalogImportList.map { it.toProductAgreementDTO(authentication )}.flatten()
    }

    private suspend fun CatalogImportExcelDTO.toProductAgreementDTO(
        authentication: Authentication?,
    ): List<ProductAgreementRegistrationDTO> {
        val cleanRef = reference.lowercase().replace("/", "-")
        val agreement = findAgreementByReferenceLike(cleanRef)
        if (agreement.agreementStatus === AgreementStatus.DELETED) {
            throw BadRequestException("Avtale med anbudsnummer ${agreement.reference} er slettet, må den opprettes?")
        }

        val supplierId = parseSupplierName(supplierName)
        val product = productRegistrationRepository.findBySupplierRefAndSupplierId(supplierRef, supplierId)
        if (!delkontraktNr.isNullOrBlank()) {
            val postRanks: List<Pair<String, Int>> = parsedelkontraktNr(delkontraktNr)

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

val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val delKontraktRegex = Regex("d(\\d+)([A-Q-STU-Z]*)r*(\\d*)")
