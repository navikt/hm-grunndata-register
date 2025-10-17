package no.nav.hm.grunndata.register.catalog

import jakarta.inject.Singleton
import java.io.InputStream
import javax.lang.model.type.UnknownTypeException
import no.nav.hm.grunndata.register.catalog.CatalogExcelFileImport.Companion.LOG
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.catalog.ColumnNames
import no.nav.hm.grunndata.register.catalog.ColumnNames.anbudsnr
import no.nav.hm.grunndata.register.catalog.ColumnNames.beskrivelse
import no.nav.hm.grunndata.register.catalog.ColumnNames.datofom
import no.nav.hm.grunndata.register.catalog.ColumnNames.datotom
import no.nav.hm.grunndata.register.catalog.ColumnNames.delkontraktnummer
import no.nav.hm.grunndata.register.catalog.ColumnNames.funksjonsendring
import no.nav.hm.grunndata.register.catalog.ColumnNames.hms_ArtNr
import no.nav.hm.grunndata.register.catalog.ColumnNames.kategori
import no.nav.hm.grunndata.register.catalog.ColumnNames.leverandorfirmanavn
import no.nav.hm.grunndata.register.catalog.ColumnNames.leverandorsted
import no.nav.hm.grunndata.register.catalog.ColumnNames.leverandørensartnr
import no.nav.hm.grunndata.register.catalog.ColumnNames.malTypeartikkel
import no.nav.hm.grunndata.register.catalog.ColumnNames.malgruppebarn

import org.apache.poi.openxml4j.util.ZipSecureFile
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

@Singleton
class CatalogExcelFileImport {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(CatalogExcelFileImport::class.java)
    }

    fun importExcelFile(
        inputStream: InputStream
    ): List<CatalogImportExcelDTO> {
        LOG.info("Reading oebs catalog xls file")
        ZipSecureFile.setMinInflateRatio(0.0)
        val workbook = WorkbookFactory.create(inputStream)
        return readProductData(workbook)

    }

    fun readProductData(
        workbook: Workbook,
    ): List<CatalogImportExcelDTO> {
        val main = workbook.getSheet("Gjeldende") ?: workbook.getSheet("gjeldende")
        LOG.info("First row num ${main.firstRowNum}")
        val columnMap = readColumnMapIndex(main.first())
        val productExcel = main.toList().mapIndexed { index, row ->
            if (index > 0) mapRowToProductAgreement(row, columnMap) else null
        }.filterNotNull()
        if (productExcel.isEmpty()) throw BadRequestException("Fant ingen produkter i Excel-fil")
        LOG.info("Total product agreements in Excel file: ${productExcel.size}")
        return productExcel
    }

    private fun mapRowToProductAgreement(
        row: Row,
        columnMap: Map<String, Int>,
    ): CatalogImportExcelDTO? {
        val leveartNr = readCellAsString(row, columnMap[leverandørensartnr.column]!!)
        val typeArtikkel = readCellAsString(row, columnMap[malTypeartikkel.column]!!)
        if ("" != leveartNr && "HMS Servicetjeneste" != typeArtikkel) {
            val hmsNr = readCellAsString(row, columnMap[hms_ArtNr.column]!!)
            val funksjonsendring = row.getCell(columnMap[funksjonsendring.column]!!).toString().trim()
            val type = mapArticleType(typeArtikkel, funksjonsendring)
            LOG.info("Mapping row to CatalogImportExcelDTO with hmsArtNr: $hmsNr, type: $type, funksjonsendring: $funksjonsendring")
            return CatalogImportExcelDTO(
                rammeavtaleHandling = readCellAsString(row, columnMap[ColumnNames.rammeavtaleHandling.column]!!),
                bestillingsNr = readCellAsString(row, columnMap[ColumnNames.bestillingsnr.column]!!),
                hmsArtNr = parseHMSNr(hmsNr),
                iso = readCellAsString(row, columnMap[kategori.column]!!),
                title = readCellAsString(row, columnMap[beskrivelse.column]!!),
                supplierRef = leveartNr,
                reference = readCellAsString(row, columnMap[anbudsnr.column]!!),
                delkontraktNr = readCellAsString(row, columnMap[delkontraktnummer.column]!!),
                dateFrom = readCellAsString(row, columnMap[datofom.column]!!),
                dateTo = readCellAsString(row, columnMap[datotom.column]!!),
                artikkelHandling = readCellAsString(row, columnMap[ColumnNames.artikkelHandling.column]!!),
                articleType = typeArtikkel,
                funksjonsendring = funksjonsendring,
                forChildren = readCellAsString(row, columnMap[malgruppebarn.column]!!),
                supplierName = readCellAsString(row, columnMap[leverandorfirmanavn.column]!!),
                supplierCity = readCellAsString(row, columnMap[leverandorsted.column]!!),
                accessory = type.accessory,
                sparePart = type.sparePart,
                mainProduct = type.mainProduct,
            )
        }
        return null
    }

    private fun readCellAsString(
        row: Row,
        column: Int,
    ): String {
        val cell = row.getCell(column)
        return DataFormatter().formatCellValue(cell).trim()
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

}

fun mapArticleType(
    articleType: String,
    funksjonsendring: String,
): ArticleType {
    val mainProduct = articleType.lowercase().indexOf("hj.middel") > -1
    val accessory =
        (articleType.lowercase().indexOf("hms del") > -1 && funksjonsendring.lowercase().indexOf("ja") > -1) ||
                mainProduct && funksjonsendring.lowercase().indexOf("ja") > -1
    val sparePart =
        articleType.lowercase().indexOf("hms del") > -1 && funksjonsendring.lowercase().indexOf("nei") > -1
    if (!mainProduct && !accessory && !sparePart) {
        throw BadRequestException("Unknown article type: $articleType")
    }
    return ArticleType(mainProduct, sparePart, accessory)
}

data class ArticleType(val mainProduct: Boolean, val sparePart: Boolean, val accessory: Boolean)