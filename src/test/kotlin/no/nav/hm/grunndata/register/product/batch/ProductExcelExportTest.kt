package no.nav.hm.grunndata.register.product.batch

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.MediaSourceType
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.rapid.dto.TechData
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.product.MediaInfoDTO
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistrationDTO
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesAttributesDTO
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistration
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.techlabel.LabelService
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Test

@MicronautTest
class ProductExcelExportTest(
    private val productExcelExport: ProductExcelExport2,
    private val supplierRepository: SupplierRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val excelExportMapper: ExcelExportMapper,
    private val seriesRepository: SeriesRegistrationRepository
) {
    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @MockBean(LabelService::class)
    fun mockTechLabelService(): LabelService = mockk<LabelService>().apply {
        every {
            fetchLabelsByIsoCode("04360901")
        } answers {
            objectMapper.readValue(techlabeljson, object : TypeReference<List<TechLabelDTO>>() {}).sortedBy { it.sort }
        }
    }


    val email = "export@test.test"
    val password = "export-123"
    val supplierId: UUID = UUID.randomUUID()
    private val supplierId2: UUID = UUID.randomUUID()
    private var testSupplier: SupplierRegistration? = null
    private var testSupplier2: SupplierRegistration? = null
    private var testSeries1: SeriesRegistration? = null


    private val techlabeljson = """
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
    fun testExcelExport() {

        val name1 = UUID.randomUUID().toString()
        val name2 = UUID.randomUUID().toString()
        runBlocking {
            testSupplier = supplierRepository.save(
                SupplierRegistration(
                    id = supplierId,
                    supplierData = SupplierData(
                        address = "address 3",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier3@test.test",
                    ),
                    identifier = name1,
                    name = name1,
                )
            )
            testSupplier2 = supplierRepository.save(
                SupplierRegistration(
                    id = supplierId2,
                    supplierData = SupplierData(
                        address = "address 4",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier4@test.test",
                    ),
                    identifier = name2,
                    name = name2
                )
            )
            userRepository.createUser(
                User(
                    email = email, token = password, name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier!!.id.toString()))
                )
            )

            val seriesId = UUID.randomUUID()

            testSeries1 =
                seriesRepository.save(
                    SeriesRegistration(
                        id = seriesId,
                        draftStatus = DraftStatus.DONE,
                        supplierId = supplierId,
                        identifier = "apitest-series-123",
                        title = "apitest-series",
                        text = "apitest-series",
                        isoCategory = "04360901",
                        status = SeriesStatus.ACTIVE,
                        adminStatus = AdminStatus.APPROVED,
                        seriesData =
                        SeriesDataDTO(
                            media = emptySet(),
                            attributes = SeriesAttributesDTO(keywords = listOf("keyword1", "keyword2")),
                        ),
                        createdBy = REGISTER,
                        updatedBy = REGISTER,
                        updatedByUser = email,
                        createdByUser = email,
                        createdByAdmin = false,
                        version = 1,
                    ),
                )
            val productData = ProductData(
                techData = listOf(TechData(key = "Lengde", unit = "cm", value = "120")),
                media = setOf(
                    MediaInfoDTO(
                        uri = "123.jpg",
                        text = "bilde av produktet",
                        source = MediaSourceType.EXTERNALURL,
                        sourceUri = "https://ekstern.url/123.jpg"
                    )
                ),
            )

            val registration = ProductRegistrationDTO(
                seriesId = seriesId.toString(),
                seriesUUID = seriesId,
                title = "Dette er produkt 1",
                articleName = "artikkelnavn",
                id = UUID.randomUUID(),
                isoCategory = "04360901",
                supplierId = testSupplier!!.id,
                hmsArtNr = "111",
                supplierRef = "eksternref-111",
                draftStatus = DraftStatus.DRAFT,
                adminStatus = AdminStatus.PENDING,
                registrationStatus = RegistrationStatus.ACTIVE,
                message = "Melding til leverandør",
                adminInfo = null,
                createdByAdmin = false,
                published = null,
                updatedByUser = email,
                createdByUser = email,
                productData = productData,
                version = 1,
                createdBy = REGISTER,
                updatedBy = REGISTER
            )

            val excelExportDtos = excelExportMapper.mapToExportDtos(listOf(registration))
            writeWorkBook(productExcelExport.createWorkbook(excelExportDtos))
        }
    }


    private fun writeWorkBook(workbook: XSSFWorkbook) {
        val fileOut = FileOutputStream("workbook.xlsx")
        workbook.write(fileOut)
        fileOut.close()
    }

}