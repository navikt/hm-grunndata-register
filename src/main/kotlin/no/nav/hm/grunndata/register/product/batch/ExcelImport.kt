package no.nav.hm.grunndata.register.product.batch

import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class ExcelImport(private val productRegistrationService: ProductRegistrationService) {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(ExcelImport::class.java)
    }

    suspend fun importExcelFileForRegistration(inputStream: InputStream): List<ProductRegistrationDTO> {
        LOG.info("Reading xls file for registration")
        val workbook = WorkbookFactory.create(inputStream)
        val productRegistrationList = readProductRegistrations(workbook)
        workbook.close()
        return productRegistrationList
    }

    private suspend fun readProductRegistrations(workbook: Workbook): List<ProductRegistrationDTO> {
        TODO() //Implement reading of product registrations from Excel
    }


}