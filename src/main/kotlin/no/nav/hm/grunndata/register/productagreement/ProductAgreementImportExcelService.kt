package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.agreement.AgreementPDTO
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationService
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.productagreement.ColumnNames.*
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.*


@Singleton
class ProductAgreementImportExcelService(private val supplierRegistrationService: SupplierRegistrationService,
                                         private val agreementRegistrationService: AgreementRegistrationService,
                                         private val productRegistrationRepository: ProductRegistrationRepository) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportExcelService::class.java)
        const val EXCEL = "EXCEL"
    }

    suspend fun importExcelFile(inputStream: InputStream): List<ProductAgreementRegistrationDTO> {
        LOG.info("Reading xls file using Apache POI")
        val workbook = WorkbookFactory.create(inputStream)
        val productAgreementList = readProductData(workbook)
        workbook.close()
        return productAgreementList
    }

    suspend fun readProductData(workbook: Workbook): List<ProductAgreementRegistrationDTO> {
        val main = workbook.getSheet("Gjeldende") ?: workbook.getSheet("gjeldende")
        LOG.info("First row num ${main.firstRowNum}")
        val columnMap = readColumnMapIndex(main.first())
        val productExcel = main.toList().mapIndexed { index, row ->
            if (index > 0) mapRowToProductAgreement(row, columnMap) else null
        }.filterNotNull()
        if (productExcel.isEmpty()) throw Exception("No product agreements found in Excel file")
        LOG.info("Total product agreements in Excel file: ${productExcel.size}")
        return productExcel.map { it.toProductAgreementDTO() }.flatten()
    }

    private suspend fun ProductAgreementExcelDTO.toProductAgreementDTO(): List<ProductAgreementRegistrationDTO> {
        val agreement = findAgreementByReference(reference)
        val supplierId =  parseSupplierName(supplierName)
        val product = productRegistrationRepository.findBySupplierRefAndSupplierId(supplierRef, supplierId)
        val postRanks: List<Pair<Int,Int>> = parsedelkontraktNr(delkontraktNr)
        return  postRanks.map { postRank ->
            ProductAgreementRegistrationDTO(
                hmsArtNr = parseHMSNr(hmsArtNr),
                agreementId = agreement.id,
                supplierRef = supplierRef,
                productId = product?.id,
                seriesUuid = product?.seriesUUID,
                title = title,
                articleName = product?.articleName,
                reference = reference,
                post = postRank.first,
                rank = postRank.second,
                supplierId = supplierId,
                published = agreement.published,
                expired = agreement.expired
            )
        }
    }

    suspend fun findAgreementByReference(reference: String): AgreementPDTO =
        agreementRegistrationService.findReferenceAndId().find {
            (it.reference.lowercase().replace("/", "-").indexOf(reference.lowercase()) > -1)
        } ?: throw Exception("Agreement $reference not found")


    private fun parseType(articleType: String): Boolean {
       return articleType.lowercase().indexOf("hms del") > -1
    }


    private fun parseSupplierName(supplierName: String): UUID = runBlocking {
        supplierRegistrationService.findNameAndId().find {
            (it.name.lowercase().indexOf(supplierName.lowercase()) > -1)
        }?.id ?: throw Exception("Supplier $supplierName not found")
    }


    private fun parsedelkontraktNr(subContractNr: String): List<Pair<Int,Int>> {
        try {
            val rankRegex = Regex("d(\\d+)r(\\d+)")
            var matchResult = rankRegex.find(subContractNr)
            val mutableList: MutableList<Pair<Int,Int>> = mutableListOf()
            while(matchResult!=null) {
                val post = rankRegex.find(subContractNr)?.groupValues?.get(1)?.toInt() ?: -1
                val rank1 = rankRegex.find(subContractNr)?.groupValues?.get(2)?.toInt() ?: -1
                mutableList.add(Pair(post, rank1))
                matchResult = matchResult.next()
            }
            return mutableList
        } catch (e: Exception) {
            LOG.error("Error parsing post and rank from $subContractNr", e)
            throw BadRequestException("Error parsing post and rank from $subContractNr")
        }
    }

    private fun parseHMSNr(hmsArtNr: String): String = hmsArtNr.substringBefore(".").toInt().toString()


    private fun mapRowToProductAgreement(row: Row, columnMap: Map<String, Int>): ProductAgreementExcelDTO? {
        val leveartNr = row.getCell(columnMap[leverandørensartnr.column]!!)?.toString()?.trim()
        val type = row.getCell(columnMap[malTypeartikkel.column]!!)?.toString()?.trim()
        if (leveartNr!=null && "" != leveartNr && type!=null && "HMS Servicetjeneste" != type) {
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
                forChildren = row.getCell(columnMap[malgruppebarn.column]!!).toString().trim(),
                supplierName = row.getCell(columnMap[leverandorfirmanavn.column]!!).toString().trim(),
                supplierCity = row.getCell(columnMap[leverandorsted.column]!!).toString().trim()
            )
        }
        return null
    }

    private fun readColumnMapIndex(firstRow: Row): Map<String, Int> = firstRow.toList().map { cell ->
        ColumnNames.values().map { getColumnIndex(cell, it.column) }
    }.flatten().filterNotNull().associate { it.first to it.second }

    private fun getColumnIndex(cell: Cell, column: String) : Pair<String, Int>? =
        if (cell.toString().replace("\\s".toRegex(), "").indexOf(column) > -1) {
            column to cell.columnIndex
        } else null
}

enum class ColumnNames(val column:String) {
    hms_ArtNr("HMS-Artnr"),
    kategori("Kategori"),
    beskrivelse("Beskrivelse"),
    leverandørensartnr("Leverandørensartnr"),
    anbudsnr("Anbudsnr"),
    delkontraktnummer("Delkontraktnummer"),
    datofom("Datofom"),
    datotom("Datotom"),
    malTypeartikkel("MalTypeartikkel"),
    malgruppebarn("Målgruppebarn"),
    leverandorfirmanavn("LeverandørFirmanavn"),
    leverandorsted("Leverandørsted")
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
    val forChildren: String,
    val supplierName: String,
    val supplierCity: String
)
