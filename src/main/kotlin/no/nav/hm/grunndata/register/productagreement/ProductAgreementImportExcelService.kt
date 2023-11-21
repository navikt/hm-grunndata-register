package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.lang.Exception
import java.util.*
import no.nav.hm.grunndata.register.productagreement.ColumnNames.*


@Singleton
class ProductAgreementImportExcelService(private val supplierRegistrationService: SupplierRegistrationService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductAgreementImportExcelService::class.java)
    }

    fun importExcelFile(inputStream: InputStream): List<ProductAgreementDTO> {
        LOG.info("Reading xls file using Apache POI")
        val workbook = WorkbookFactory.create(inputStream)
        val productAgreementList = readProductData(workbook)
        workbook.close()
        inputStream.close()
        return productAgreementList
    }

    fun readProductData(workbook: Workbook): List<ProductAgreementDTO> {
        val main = workbook.getSheet("Gjeldende") ?: workbook.getSheet("gjeldende")
        LOG.info("First row num ${main.firstRowNum}")
        val columnMap = readColumnMapIndex(main.first())
        val productExcel = main.toList().mapIndexed { index, row ->
            if (index > 0) mapRowToProductAgreement(row, columnMap) else null
        }.filterNotNull()
        LOG.info("Total product agreements in Excel file: ${productExcel.size}")
        return productExcel.map { it.toProductAgreementDTO() }.toList()
    }

    private fun ProductAgreementExcelDTO.toProductAgreementDTO(): ProductAgreementDTO {
        return ProductAgreementDTO(
            hmsArtNr = parseHMSNr(hmsArtNr),
            text = text,
            supplierRef = supplierRef,
            reference = reference,
            type = articleType,
            post = parsePost(subContractNr),
            rank = parseRank(subContractNr),
            supplierId = parseSupplierName(supplierName),
            supplierName = this.supplierName
        )
    }


    private fun parseSupplierName(supplierName: String): UUID = runBlocking {
        supplierRegistrationService.findNameAndId().find {
            (it.name.lowercase().indexOf(supplierName.lowercase()) > -1)
        }?.id ?: throw Exception("Supplier $supplierName not found")
    }


    private fun parseRank(subContractNr: String): Int {
        return try {
            val rankRegex = Regex("d(\\d+)r(\\d+)")
            rankRegex.find(subContractNr)?.groupValues?.get(2)?.toInt() ?: 99
        } catch (e: Exception) {
            return 99
        }
    }

    private fun parsePost(subContractNr: String): Int {
        return try {
            subContractNr.toInt()
        } catch (e: NumberFormatException) {
            val postRegex = Regex("d(\\d+)")
            postRegex.find(subContractNr)?.groupValues?.get(1)?.toInt() ?: -1
        }
    }


    private fun parseHMSNr(hmsArtNr: String): String = hmsArtNr.substringBefore(".").toInt().toString()


    private fun mapRowToProductAgreement(row: Row, columnMap: Map<String, Int>): ProductAgreementExcelDTO? {
        val leveartNr = row.getCell(columnMap[leverandorfirmanavn.column]!!)?.toString()
        val type = row.getCell(columnMap[malTypeartikkel.column]!!)?.toString()
        if (leveartNr != null && "" != leveartNr && "HMS Servicetjeneste" != type) {
            return ProductAgreementExcelDTO(
                hmsArtNr = row.getCell(columnMap[hms_ArtNr.column]!!).toString(),
                iso = row.getCell(columnMap[kategori.column]!!).toString(),
                text = row.getCell(columnMap[beskrivelse.column]!!).toString(),
                supplierRef = leveartNr!!,
                reference = row.getCell(columnMap[anbudsnr.column]!!).toString(),
                subContractNr = row.getCell(columnMap[delkontraktnummer.column]!!).toString(),
                dateFrom = row.getCell(columnMap[datofom.column]!!).toString(),
                dateTo = row.getCell(columnMap[datotom.column]!!).toString(),
                articleType = type!!,
                targetGroup = row.getCell(columnMap[malgruppebarn.column]!!).toString(),
                supplierName = row.getCell(columnMap[leverandorfirmanavn.column]!!).toString(),
                supplierCity = row.getCell(columnMap[leverandorsted.column]!!).toString()
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
    val text: String,
    val supplierRef: String,
    val reference: String,
    val subContractNr: String,
    val dateFrom: String,
    val dateTo: String,
    val articleType: String,
    val targetGroup: String,
    val supplierName: String,
    val supplierCity: String
) {

    override fun toString(): String =
        "hmsArtNr: $hmsArtNr, iso: $iso, text: $text, supplierRef: $supplierRef, tenderNr: $reference, " +
                "subContractNr: $subContractNr, dateFrom: $dateFrom, dateTo: $dateTo, articleType: $articleType, " +
                "targetGroup: $targetGroup, supplierName: $supplierName, supplierCity: $supplierCity"

}

data class ProductAgreementDTO(
    val hmsArtNr: String,
    val text: String,
    val supplierRef: String,
    val type: String,
    val reference: String,
    val post: Int,
    val rank: Int,
    val supplierId: UUID,
    val supplierName: String,
) {
    override fun toString(): String {
        return "hmsArtNr: $hmsArtNr, text: $text, supplierRef: $supplierRef, reference: $reference, type: $type, " +
                "post: $post, rank: $rank, supplierId: $supplierId, supplierName: $supplierName"
    }
}