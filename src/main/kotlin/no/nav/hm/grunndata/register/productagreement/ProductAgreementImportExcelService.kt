package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
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
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory

@Singleton
class ProductAgreementImportExcelService(
    private val supplierRegistrationService: SupplierRegistrationService,
    private val agreementRegistrationService: AgreementRegistrationService,
    private val productRegistrationRepository: ProductRegistrationRepository,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportExcelService::class.java)
        const val EXCEL = "EXCEL"
    }

    suspend fun importExcelFile(inputStream: InputStream): List<ProductAgreementRegistrationDTO> {
        LOG.info("Reading xls file")
        val workbook = WorkbookFactory.create(inputStream)
        val productAgreementList = readProductData(workbook)
        workbook.close()
        return productAgreementList
    }

    suspend fun readProductData(workbook: Workbook): List<ProductAgreementRegistrationDTO> {
        val main = workbook.getSheet("Gjeldende") ?: workbook.getSheet("gjeldende")
        LOG.info("First row num ${main.firstRowNum}")
        val columnMap = readColumnMapIndex(main.first())
        val productExcel =
            main.toList().mapIndexed { index, row ->
                if (index > 0) mapRowToProductAgreement(row, columnMap) else null
            }.filterNotNull()
        if (productExcel.isEmpty()) throw BadRequestException("Fant ingen produkter i Excel-fil")
        LOG.info("Total product agreements in Excel file: ${productExcel.size}")
        return productExcel.map { it.toProductAgreementDTO() }.flatten()
    }

    private fun mapArticleType(articleType: String, funksjonsendring: String): ArticleType {
        val mainProduct = articleType.lowercase().indexOf("hms hj.middel") > -1
        val accessory = articleType.lowercase().indexOf("hms del") > -1 && funksjonsendring.lowercase().indexOf("ja") > -1
        val sparePart = articleType.lowercase().indexOf("hms del") > -1 && funksjonsendring.lowercase().indexOf("nei") > -1
        return ArticleType(mainProduct, sparePart, accessory)
    }

    private suspend fun ProductAgreementExcelDTO.toProductAgreementDTO(): List<ProductAgreementRegistrationDTO> {
        val cleanRef = reference.lowercase().replace("/", "-")
        val agreement = findAgreementByReference(cleanRef)
        if (agreement.agreementStatus === AgreementStatus.DELETED) {
            throw BadRequestException("Avtale med anbudsnummer ${agreement.reference} er slettet, må den opprettes?")
        }
        if (agreement.agreementStatus === AgreementStatus.ACTIVE) {
            throw BadRequestException("Avtale med anbudsnummer ${agreement.reference} er publisert")
        }
        val supplierId = parseSupplierName(supplierName)
        var product = productRegistrationRepository.findBySupplierRefAndSupplierId(supplierRef, supplierId)
        val type = mapArticleType(articleType, funksjonsendring)
        val postRanks: List<Pair<String, Int>> = parsedelkontraktNr(delkontraktNr)
        return postRanks.map { postRank ->
            LOG.info("Creating product agreement for agreement $cleanRef, post ${postRank.first}, rank ${postRank.second}")
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
                sparePart = type.sparePart,
                accessory = type.accessory
            )
        }
    }

    suspend fun findAgreementByReference(reference: String): AgreementRegistrationDTO =
        agreementRegistrationService.findByReference(reference)
            ?: throw BadRequestException("Avtale $reference finnes ikke, må den opprettes?")

    private fun parseType(articleType: String): Boolean {
        return articleType.lowercase().indexOf("hms del") > -1
    }

    private fun parseSupplierName(supplierName: String): UUID =
        runBlocking {
            supplierRegistrationService.findNameAndId().find {
                (it.name.lowercase().indexOf(supplierName.lowercase()) > -1)
            }?.id ?: throw BadRequestException("Leverandør $supplierName finnes ikke i registeret, må den opprettes?")
        }

    private fun parsedelkontraktNr(subContractNr: String): List<Pair<String, Int>> {
        try {
            val rankRegex = Regex("(?i)d(\\d+)([A-Z]*)r(\\d+)")
            var matchResult = rankRegex.find(subContractNr)
            val mutableList: MutableList<Pair<String, Int>> = mutableListOf()
            if (matchResult != null) {
                while (matchResult != null) {
                    val groupValues = rankRegex.find(subContractNr)?.groupValues
                    val post = groupValues?.get(1) + groupValues?.get(2)?.uppercase()
                    val rank1 = groupValues?.get(3)?.toInt() ?: 99
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

    private fun mapRowToProductAgreement(
        row: Row,
        columnMap: Map<String, Int>,
    ): ProductAgreementExcelDTO? {
        val leveartNr = row.getCell(columnMap[leverandørensartnr.column]!!)?.toString()?.trim()
        val type = row.getCell(columnMap[malTypeartikkel.column]!!)?.toString()?.trim()
        if (leveartNr != null && "" != leveartNr && type != null && "HMS Servicetjeneste" != type) {
            return ProductAgreementExcelDTO(
                hmsArtNr = row.getCell(columnMap[hms_ArtNr.column]!!).toString().trim(),
                iso = row.getCell(columnMap[kategori.column]!!).toString().trim(),
                title = row.getCell(columnMap[beskrivelse.column]!!).toString().trim(),
                supplierRef = leveartNr,
                reference = row.getCell(columnMap[anbudsnr.column]!!).toString().trim(),
                delkontraktNr = row.getCell(columnMap[delkontraktnummer.column]!!).toString().trim(),
                dateFrom = row.getCell(columnMap[datofom.column]!!).toString().trim(),
                dateTo = row.getCell(columnMap[datotom.column]!!).toString().trim(),
                articleType = type,
                funksjonsendring = row.getCell(columnMap[funksjonsendring.column]!!).toString().trim(),
                forChildren = row.getCell(columnMap[malgruppebarn.column]!!).toString().trim(),
                supplierName = row.getCell(columnMap[leverandorfirmanavn.column]!!).toString().trim(),
                supplierCity = row.getCell(columnMap[leverandorsted.column]!!).toString().trim(),
            )
        }
        return null
    }

    private fun readColumnMapIndex(firstRow: Row): Map<String, Int> =
        firstRow.toList().map { cell ->
            ColumnNames.values().map { getColumnIndex(cell, it.column) }
        }.flatten().filterNotNull().associate { it.first to it.second }

    private fun getColumnIndex(
        cell: Cell,
        column: String,
    ): Pair<String, Int>? =
        if (cell.toString().replace("\\s".toRegex(), "").indexOf(column) > -1) {
            column to cell.columnIndex
        } else {
            null
        }

    data class ArticleType(val mainProduct: Boolean, val sparePart: Boolean, val accessory: Boolean)
}

enum class ColumnNames(val column: String) {
    hms_ArtNr("HMS-Artnr"),
    kategori("Kategori"),
    beskrivelse("Beskrivelse"),
    leverandørensartnr("Leverandørensartnr"),
    anbudsnr("Anbudsnr"),
    delkontraktnummer("Delkontraktnummer"),
    datofom("Datofom"),
    datotom("Datotom"),
    malTypeartikkel("MalTypeartikkel"),
    funksjonsendring("Funksjonsendring"),
    malgruppebarn("Målgruppebarn"),
    leverandorfirmanavn("LeverandørFirmanavn"),
    leverandorsted("Leverandørsted"),
}

data class ProductAgreementExcelDTO(
    val hmsArtNr: String,
    val iso: String,
    val title: String,
    val supplierRef: String,
    val reference: String,
    val delkontraktNr: String,
    val dateFrom: String,
    val dateTo: String,
    val articleType: String,
    val funksjonsendring: String,
    val forChildren: String,
    val supplierName: String,
    val supplierCity: String,
)

fun String.extractDelkontraktNrFromTitle(): String? {
    val regex = """(\d+)([A-Z]*)([.|:])""".toRegex()
    return regex.find(this)?.groupValues?.get(0)?.dropLast(1)
}
