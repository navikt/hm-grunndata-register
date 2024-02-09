package no.nav.hm.grunndata.register.product.batch

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.techlabel.LabelService
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

@Singleton
class ProductExcelImport(private val labelService: LabelService) {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ProductExcelImport::class.java)
    }

    fun importExcelFileForRegistration(inputStream: InputStream): List<ProductRegistrationExcelDTO> {
        println("Reading xls file for registration")
        val workbook = WorkbookFactory.create(inputStream)
        val productRegistrationList = readProductRegistrations(workbook)
        workbook.close()
        return productRegistrationList
    }

    private fun readProductRegistrations(workbook: Workbook): List<ProductRegistrationExcelDTO> {
        val products = workbook.sheetIterator().asSequence().toList().map { sheet ->
            val isoCategory = if (sheet.sheetName.startsWith("\""))
                sheet.sheetName.replace("\"","") else sheet.sheetName
            val techLabels = labelService.fetchLabelsByIsoCode(isoCategory).map { it.label }
            LOG.info("Fetching labels for isoCategory: $isoCategory, got: ${techLabels.size}")
            val firstRow = sheet.getRow(0)
            val headerNames = if (oldVersion(firstRow))
                HeaderTitleOld.values().map { it.label } + techLabels
            else HeaderTitleNew.values().map { it.label } + techLabels
            val headerMap = readHeaderMapIndex(firstRow, headerNames)
            sheet.toList().mapIndexed { index, row ->
                if (index > 1) mapRowToProductRegistration(row, headerMap, isoCategory, techLabels) else null
            }.filterNotNull()
        }
        return products.flatten()
    }

    private fun mapRowToProductRegistration(row: Row, headerMap: Map<String, Int>, isoCategory: String, techLabels: List<String>): ProductRegistrationExcelDTO {
        return ProductRegistrationExcelDTO (
            isoCategory = isoCategory,
            produktserieid = row.getCell(headerMap[HeaderTitleNew.produKtserieid.label]!!).toString().trim(),
            produktseriesnavn = row.getCell(headerMap[HeaderTitleNew.produktseriesnavn.label]!!).toString().trim(),
            produktseriebeskrivelse = row.getCell(headerMap[HeaderTitleNew.produktseriebeskrivelse.label]!!).toString().trim(),
            produktid = row.getCell(headerMap[HeaderTitleNew.produktid.label]!!).toString().trim(),
            hmsnr = row.getCell(headerMap[HeaderTitleNew.hmsnr.label]!!).toString().trim(),
            produktnavn = row.getCell(headerMap[HeaderTitleNew.produktnavn.label]!!).toString().trim(),
            andrespesifikasjoner = row.getCell(headerMap[HeaderTitleNew.andrespesifikasjoner.label]!!).toString().trim(),
            levartnr = row.getCell(headerMap[HeaderTitleNew.levartnr.label]!!).toString().trim(),
            leverandorid = row.getCell(headerMap[HeaderTitleNew.leverandorid.label]!!).toString().trim(),
            delkontrakt = row.getCell(headerMap[HeaderTitleNew.delkontrakt.label]?:headerMap[HeaderTitleOld.postid.label]!!).toString().trim(),
            rangering = row.getCell(headerMap[HeaderTitleNew.rangering.label]!!).toString().trim(),
            techData = techLabels.map { it to row.getCell(headerMap[it]!!).toString().trim() }.toMap()
        )
    }


    private fun oldVersion(firstRow: Row): Boolean {
        firstRow.toList().forEach { cell ->
            if (cell.toString().indexOf("Post-id") > -1) return true
        }
        return false
    }

    private fun readHeaderMapIndex(firstRow: Row, headerNames: List<String>): Map<String, Int > =
        headerNames.map { getColumnIndex(firstRow.toList(), it) }.associate { it.first to it.second }


    private fun getColumnIndex(cells: List<Cell>, column: String): Pair<String, Int> {
        for (cell in cells) {
            if (cell.toString().indexOf(column) > -1 ){
                println(column)
                return column to cell.columnIndex
            }
        }
        throw BadRequestException("Header cells is missing for column: $column")
    }
}


data class ProductRegistrationExcelDTO(
    val isoCategory: String,
    val produktserieid: String?,
    val produktseriesnavn: String?,
    val produktseriebeskrivelse: String?,
    val produktid: String?,
    val hmsnr: String?,
    val produktnavn: String?,
    val andrespesifikasjoner: String?,
    val levartnr: String,
    val leverandorid: String,
    val delkontrakt: String?,
    val rangering: String?,
    val techData: Map<String, String> = emptyMap()
)
