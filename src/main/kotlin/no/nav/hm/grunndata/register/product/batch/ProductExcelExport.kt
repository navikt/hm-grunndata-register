package no.nav.hm.grunndata.register.product.batch

import jakarta.inject.Singleton
import java.io.OutputStream
import no.nav.hm.grunndata.register.techlabel.LabelService
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

@Singleton
class ProductExcelExport2(private val labelService: LabelService) {

    fun createWorkbookToOutputStream(productExcelExportDtos: List<ProductExcelExportDto>, out: OutputStream) {
        val workbook = createWorkbook(productExcelExportDtos)
        workbook.write(out)
    }

    fun createWorkbook(productExcelExportDtos: List<ProductExcelExportDto>): XSSFWorkbook {
        val isoGroups = productExcelExportDtos.groupBy { it.isoCategory }
        val isoKeys = isoGroups.keys.toList().sorted()
        val workbook = XSSFWorkbook()
        isoKeys.forEach { iso ->
            val techlabels = labelService.fetchLabelsByIsoCode(iso)

            val sheet = workbook.createSheet(iso.ifEmpty { "Ingen ISO kode" })
            createHeaderRows(sheet, techlabels)
            createProductRow(sheet, isoGroups[iso]!!, techlabels)
        }
        return workbook
    }


    private fun createHeaderRows(sheet: XSSFSheet, techLabels: List<TechLabelDTO>) {
        val headerRow = sheet.createRow(0)
        val commentRow = sheet.createRow(1)

        HeaderTitleNew.entries.forEachIndexed { index, title ->
            val headerCell = headerRow.createCell(index)
            val commentCell = commentRow.createCell(index)
            headerCell.setCellValue(title.label)
            commentCell.setCellValue(title.comment)
        }
        var index = HeaderTitleNew.entries.size
        for (techLabelDTO in techLabels) {
            val headerCell = headerRow.createCell(index)
            val commentCell = commentRow.createCell(index)
            headerCell.setCellValue(techLabelDTO.label)
            if (!techLabelDTO.unit.isNullOrEmpty())
                commentCell.setCellValue(techLabelDTO.unit)
            else if (techLabelDTO.type == "L") commentCell.setCellValue("JA/NEI")
            else commentCell.setCellValue(techLabelDTO.definition ?: "")
            index++
        }
    }

    private fun createProductRow(
        sheet: XSSFSheet,
        productExcelExportDtos: List<ProductExcelExportDto>,
        techLabels: List<TechLabelDTO>
    ) {
        productExcelExportDtos.forEach { excelExport ->
            excelExport.products.forEachIndexed { index, product ->
                val techDataGroup = product.productData.techData.associateBy { it.key }
                val productRow = sheet.createRow(index + 2)

                productRow.createCell(0).setCellValue(excelExport.seriesUuid)
                productRow.createCell(1).setCellValue(excelExport.seriesTitle)
                productRow.createCell(2).setCellValue(excelExport.seriesDescription)

                productRow.createCell(3).setCellValue(product.id.toString())
                productRow.createCell(4).setCellValue(product.hmsArtNr ?: "")
                productRow.createCell(5).setCellValue(product.articleName)
                productRow.createCell(6).setCellValue(product.productData.attributes.shortdescription)
                productRow.createCell(7).setCellValue(product.supplierRef)
                productRow.createCell(8).setCellValue(product.supplierId.toString())
                productRow.createCell(9)
                    .setCellValue(if (product.agreements.isNotEmpty()) product.agreements[0].postNr.toString() else "")
                productRow.createCell(10)
                    .setCellValue(if (product.agreements.isNotEmpty()) product.agreements[0].rank.toString() else "")
                techLabels.forEachIndexed { z, techLabelDTO ->
                    val techLabelCell = productRow.createCell(z + 11)
                    techLabelCell.setCellValue(techDataGroup[techLabelDTO.label]?.value ?: "")
                }
            }
        }
    }
}

enum class HeaderTitleNew(val label: String, val comment: String) {
    produKtserieid("Produktserie id", "Påkrevd, Kan ikke endres"),
    produktseriesnavn("Produktserie navn", "Kan ikke endres"),
    produktseriebeskrivelse("Produktserie beskrivelse", "Kan ikke endres"),
    produktid("Produkt-id", "Tom hvis nytt produkt"),
    hmsnr("HMS-nr.", "Kan ikke endres"),
    produktnavn("Produktnavn", "Påkrevd"),
    andrespesifikasjoner("Andre spesifikasjoner", ""),
    levartnr("Lev-artnr.", "Påkrevd, kan ikke endres"),
    leverandorid("Leverandør-id", "Påkreved, Kan ikke endres"),
    delkontrakt("Delkontrakt", "Kan ikke endres"),
    rangering("Rangering", "Kan ikke endres")
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