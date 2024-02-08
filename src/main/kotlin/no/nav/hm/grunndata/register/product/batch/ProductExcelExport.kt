package no.nav.hm.grunndata.register.product.batch

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.techlabel.LabelService
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

@Singleton
class ProductExcelExport(private val labelService: LabelService) {


    fun createWorkbookToOutputStream(products: List<ProductRegistrationDTO>, out: OutputStream){
        val workbook = createWorkbook(products)
        workbook.write(out)
    }

    fun createWorkbook(products: List<ProductRegistrationDTO>): XSSFWorkbook {
        val isoGroups = products.groupBy { it.isoCategory }
        val isoKeys = isoGroups.keys.toList().sorted()
        val workbook = XSSFWorkbook()
        isoKeys.forEachIndexed { index, iso ->
            val techlabels = labelService.fetchLabelsByIsoCode(iso)
            val sheet = workbook.createSheet(iso)
            createHeaderRows(sheet, iso, techlabels)
            createProductRow(sheet, isoGroups[iso]!!, techlabels)
        }
        return workbook
    }

    private fun createHeaderRows(sheet: XSSFSheet, iso: String, techLabels: List<TechLabelDTO>): Row {
        val headerRow = sheet.createRow(0)
        var index = 0;
        for (title in HeaderTitleNew.values())  {
            val headerCell = headerRow.createCell(index++)
            headerCell.setCellValue(title.label)
        }
        for (techLabelDTO in techLabels) {
            val headerCell = headerRow.createCell(index++)
            headerCell.setCellValue(techLabelDTO.label +" ("+techLabelDTO.unit+")")
        }
        val commentRow = sheet.createRow(1)
        commentRow.createCell(0).setCellValue("Kommentar")
        return commentRow
    }

    private fun createProductRow(sheet: XSSFSheet, products: List<ProductRegistrationDTO>,techLabels: List<TechLabelDTO>) {
        products.forEachIndexed { index, product ->
            val techDataGroup = product.productData.techData.associateBy{ it.key }
            val productRow = sheet.createRow(index + 2)
            productRow.createCell(0).setCellValue(product.seriesUUID.toString())
            productRow.createCell(1).setCellValue(product.title)
            productRow.createCell(2).setCellValue(product.id.toString())
            productRow.createCell(3).setCellValue(product.hmsArtNr)
            productRow.createCell(4).setCellValue(product.articleName)
            productRow.createCell(5).setCellValue(product.supplierRef)
            productRow.createCell(6).setCellValue(product.supplierId.toString())
            productRow.createCell(7).setCellValue(if (product.agreements.isNotEmpty()) product.agreements[0].postNr.toString() else "")
            productRow.createCell(8).setCellValue(if (product.agreements.isNotEmpty()) product.agreements[0].rank.toString() else "")
            techLabels.forEachIndexed { z, techLabelDTO ->
                val techLabelCell = productRow.createCell(z+9)
                techLabelCell.setCellValue(techDataGroup[techLabelDTO.label]?.value?:"")
            }
        }
    }
}

enum class HeaderTitleNew(val label: String) {
    produKtserieid("Produktserie id"),
    produktseriesnavn("Produktserie navn"),
    produktseriebeskrivelse("Produktserie beskrivelse"),
    produktid("Produkt-id"),
    hmsnr("HMS-nr."),
    produktnavn("Produktnavn"),
    andrespesifikasjoner("Andre spesifikasjoner"),
    levartnr("Lev-artnr."),
    leverandorid("Leverandør-id"),
    delkontrakt("Delkontrakt"),
    rangering("Rangering")
}

enum class HeaderTitleOld(val label: String) {
    produktserieid("Produktserie id"),
    produktserienavn("Produktserie navn"),
    produktseriebeskrivelse("Produktserie beskrivelse"),
    produktid("Produkt-id"),
    hmsnr("HMS-nr."),
    produktnavn("Produktnavn"),
    andrespesifikasjoner("Andre spesifikasjoner"),
    levartnr("Lev-artnr."),
    leverandorid("Leverandør-id"),
    postid("Post-id"),
    rangering("Rangering")
}
