package no.nav.hm.grunndata.register.product.batch

import jakarta.inject.Singleton
import java.io.InputStream
import java.util.UUID
import no.nav.helse.rapids_rivers.toUUID
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.error.BadRequestException
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationDryRunDTO
import no.nav.hm.grunndata.register.techlabel.LabelService
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

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
        val products =
            workbook.sheetIterator().asSequence().toList().map { sheet ->
                val isoCategory =
                    if (sheet.sheetName.startsWith("\"")) {
                        sheet.sheetName.replace("\"", "")
                    } else {
                        sheet.sheetName
                    }
                val techLabels = labelService.fetchLabelsByIsoCode(isoCategory)
                val labelList = techLabels.map { it.label }
                LOG.info("Fetching labels for isoCategory: $isoCategory, got: ${techLabels.size}")
                val firstRow = sheet.getRow(0)
                val headerNames =
                    if (oldVersion(firstRow)) {
                        HeaderTitleOld.values().map { it.label } + labelList
                    } else {
                        HeaderTitleNew.values().map { it.label } + labelList
                    }
                val headerMap = readHeaderMapIndex(firstRow, headerNames)
                sheet.toList().mapIndexed { index, row ->
                    if (index > 1) mapRowToProductRegistration(row, headerMap, isoCategory, techLabels) else null
                }.filterNotNull()
            }
        return products.flatten()
    }

    private fun mapRowToProductRegistration(
        row: Row,
        headerMap: Map<String, Int>,
        isoCategory: String,
        techLabels: List<TechLabelDTO>,
    ): ProductRegistrationExcelDTO {
        return ProductRegistrationExcelDTO(
            isoCategory = isoCategory,
            produktserieid = row.getCell(headerMap[HeaderTitleNew.produKtserieid.label]!!).toString().trim(),
            produktseriesnavn = row.getCell(headerMap[HeaderTitleNew.produktseriesnavn.label]!!).toString().trim(),
            produktseriebeskrivelse =
                row.getCell(headerMap[HeaderTitleNew.produktseriebeskrivelse.label]!!)?.toString()
                    ?.trim(),
            produktid = row.getCell(headerMap[HeaderTitleNew.produktid.label]!!)?.toString()?.trim(),
            hmsnr = row.getCell(headerMap[HeaderTitleNew.hmsnr.label]!!)?.toString()?.trim(),
            produktnavn = row.getCell(headerMap[HeaderTitleNew.produktnavn.label]!!).toString().trim(),
            andrespesifikasjoner =
                row.getCell(headerMap[HeaderTitleNew.andrespesifikasjoner.label]!!)?.toString()
                    ?.trim(),
            levartnr = row.getCell(headerMap[HeaderTitleNew.levartnr.label]!!).toString().trim(),
            leverandorid = row.getCell(headerMap[HeaderTitleNew.leverandorid.label]!!).toString().trim(),
            delkontrakt =
                row.getCell(
                    headerMap[HeaderTitleNew.delkontrakt.label] ?: headerMap[HeaderTitleOld.postid.label]!!,
                )?.toString()?.trim(),
            rangering = row.getCell(headerMap[HeaderTitleNew.rangering.label]!!)?.toString()?.trim(),
            techData =
                techLabels.map {
                    TechData(
                        key = it.label,
                        value = row.getCell(headerMap[it.label]!!).toString().trim(),
                        unit = it.unit ?: "",
                    )
                },
        )
    }

    private fun oldVersion(firstRow: Row): Boolean {
        firstRow.toList().forEach { cell ->
            if (cell.toString().indexOf("Post-id") > -1) return true
        }
        return false
    }

    private fun readHeaderMapIndex(
        firstRow: Row,
        headerNames: List<String>,
    ): Map<String, Int> = headerNames.map { getColumnIndex(firstRow.toList(), it) }.associate { it.first to it.second }

    private fun getColumnIndex(
        cells: List<Cell>,
        column: String,
    ): Pair<String, Int> {
        for (cell in cells) {
            if (cell.toString().indexOf(column) > -1) {
                println(column)
                return column to cell.columnIndex
            }
        }
        throw BadRequestException("Header cells is missing for column: $column")
    }
}

data class ProductRegistrationExcelDTO(
    val isoCategory: String,
    val produktserieid: String,
    val produktseriesnavn: String,
    val produktseriebeskrivelse: String?,
    val produktid: String?,
    val hmsnr: String?,
    val produktnavn: String,
    val andrespesifikasjoner: String?,
    val levartnr: String,
    val leverandorid: String,
    val delkontrakt: String?,
    val rangering: String?,
    val techData: List<TechData> = emptyList(),
)

fun ProductRegistrationExcelDTO.toRegistrationDTO(): ProductRegistrationDTO {
    val productId = produktid?.toUUID() ?: UUID.randomUUID()
    val seriesUUID = produktserieid.toUUID() ?: productId
    val supplierId = leverandorid.toUUID()
    return ProductRegistrationDTO(
        id = productId,
        seriesId = seriesUUID.toString(),
        seriesUUID = seriesUUID,
        supplierId = supplierId,
        supplierRef = levartnr,
        hmsArtNr = hmsnr,
        draftStatus = DraftStatus.DRAFT,
        registrationStatus = RegistrationStatus.ACTIVE,
        adminStatus = AdminStatus.PENDING,
        title = produktseriesnavn,
        articleName = produktnavn ?: produktseriesnavn ?: "",
        isoCategory = isoCategory,
        sparePart = false,
        accessory = false,
        productData =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = andrespesifikasjoner,
                        text = produktseriebeskrivelse,
                    ),
                techData = techData,
            ),
    )
}

fun ProductRegistrationExcelDTO.toRegistrationDryRunDTO(): ProductRegistrationDryRunDTO {
    val productId = produktid?.toUUID()
    val seriesUUID = produktserieid?.toUUID() ?: productId
    val supplierId = leverandorid.toUUID()
    return ProductRegistrationDryRunDTO(
        id = productId,
        seriesUUID = seriesUUID,
        supplierId = supplierId,
        supplierRef = levartnr,
        hmsArtNr = hmsnr,
        draftStatus = DraftStatus.DRAFT,
        registrationStatus = RegistrationStatus.ACTIVE,
        adminStatus = AdminStatus.PENDING,
        title = produktseriesnavn ?: produktnavn ?: "",
        articleName = produktnavn ?: produktseriesnavn ?: "",
        isoCategory = isoCategory,
        productData =
            ProductData(
                attributes =
                    Attributes(
                        shortdescription = andrespesifikasjoner,
                        text = produktseriebeskrivelse,
                    ),
                techData = techData,
            ),
    )
}

fun ProductRegistrationDTO.toProductRegistrationDryRunDTO(): ProductRegistrationDryRunDTO =
    ProductRegistrationDryRunDTO(
        id = id,
        seriesUUID = seriesUUID,
        supplierId = supplierId,
        supplierRef = supplierRef,
        hmsArtNr = hmsArtNr,
        title = title,
        articleName = articleName,
        draftStatus = draftStatus,
        adminStatus = adminStatus,
        registrationStatus = registrationStatus,
        productData = productData,
        isoCategory = isoCategory,
    )
