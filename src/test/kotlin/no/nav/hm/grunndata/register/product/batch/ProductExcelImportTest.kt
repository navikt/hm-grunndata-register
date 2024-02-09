package no.nav.hm.grunndata.register.product.batch

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import no.nav.hm.grunndata.register.techlabel.LabelService
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO
import org.junit.jupiter.api.Test


@MicronautTest
class ProductExcelImportTest(private val excelImport: ProductExcelImport,
                             private val objectMapper: ObjectMapper) {

    @MockBean(LabelService::class)
    fun mockTechLabelService(): LabelService = mockk<LabelService>().apply {
        every {
            fetchLabelsByIsoCode("04360901")
        } answers {
            objectMapper.readValue(techlabeljson, object : TypeReference<List<TechLabelDTO>>() {}).sortedBy { it.sort }
        }
    }

    val techlabeljson = """
        [
          {
            "id": "9736854b-9373-4a77-95e7-dcc9de3497cd",
            "identifier": "HMDB-21904",
            "label": "Beregnet på barn",
            "guide": "Beregnet på barn",
            "isocode": "043609",
            "type": "L",
            "unit": "",
            "sort": 5,
            "createdBy": "HMDB",
            "updatedBy": "HMDB",
            "created": "2024-02-06T11:58:09.573083",
            "updated": "2024-02-06T11:58:09.573084"
          },
          {
            "id": "b4c7e4ef-d0cd-41f7-8c20-aa36fbbc0882",
            "identifier": "HMDB-19680",
            "label": "Lengde",
            "guide": "Lengde",
            "isocode": "043609",
            "type": "N",
            "unit": "cm",
            "sort": 1,
            "createdBy": "HMDB",
            "updatedBy": "HMDB",
            "created": "2024-02-06T11:58:09.574848",
            "updated": "2024-02-06T11:58:09.574849"
          },
          {
            "id": "9c659c80-a970-4d13-842f-35114925f792",
            "identifier": "HMDB-20356",
            "label": "Bredde",
            "guide": "Bredde",
            "isocode": "043609",
            "type": "N",
            "unit": "cm",
            "sort": 2,
            "createdBy": "HMDB",
            "updatedBy": "HMDB",
            "created": "2024-02-06T11:58:09.574855",
            "updated": "2024-02-06T11:58:09.574856"
          },
          {
            "id": "e20ca848-a44b-42ab-8af2-21aed56f3916",
            "identifier": "HMDB-20773",
            "label": "Vekt",
            "guide": "Vekt",
            "isocode": "043609",
            "type": "N",
            "unit": "kg",
            "sort": 3,
            "createdBy": "HMDB",
            "updatedBy": "HMDB",
            "created": "2024-02-06T11:58:09.574859",
            "updated": "2024-02-06T11:58:09.57486"
          },
          {
            "id": "f74e379f-82b0-4efd-a43d-658048c839a2",
            "identifier": "HMDB-22352",
            "label": "Kulediameter",
            "guide": "Kulediameter",
            "isocode": "043609",
            "type": "N",
            "unit": "mm",
            "sort": 4,
            "createdBy": "HMDB",
            "updatedBy": "HMDB",
            "created": "2024-02-06T11:58:09.574864",
            "updated": "2024-02-06T11:58:09.574865"
          }
        ]
    """.trimIndent()


    @Test
    fun testExcelImport() {
        val inputStream = javaClass.classLoader.getResourceAsStream("import/import.xls")
        val productRegistrationList = excelImport.importExcelFileForRegistration(inputStream)
        println(objectMapper.writeValueAsString(productRegistrationList))
    }

}

