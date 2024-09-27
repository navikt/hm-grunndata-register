package substituttlister

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.io.IOException

class ParseSubstituttliste {
    @Throws(IOException::class)
    fun readExcel(filePath: String?): List<List<String>> {
        val fis = FileInputStream(filePath)
        val workbook: Workbook = XSSFWorkbook(fis)
        val sheet = workbook.getSheetAt(0)
        val groups: MutableList<List<String>> = ArrayList()
        val currentGroup: MutableList<String> = ArrayList()

        var isFirstRow = true

        for (row in sheet) {
            // Skip the first row
            if (isFirstRow) {
                isFirstRow = false
                continue
            }
            val cell = row.getCell(3) // Column D
            if (cell != null && cell.toString().trim { it <= ' ' }.isNotEmpty()) {
                var cellValue = cell.toString()
                if (cellValue.endsWith(".0")) {
                    cellValue = cellValue.substring(0, cellValue.length - 2)
                }
                currentGroup.add(cellValue)
            } else if (currentGroup.isNotEmpty()) {
                groups.add(ArrayList(currentGroup))
                currentGroup.clear()
            }
        }
        if (currentGroup.isNotEmpty()) {
            groups.add(currentGroup)
        }
        workbook.close()
        fis.close()
        return groups
    }
}
