package no.nav.hm.grunndata.register.product.batch

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
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
import no.nav.hm.grunndata.register.product.ProductDTOMapper
import no.nav.hm.grunndata.register.product.ProductData
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationRepository
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesAttributesDTO
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationRepository
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistration
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test

@MicronautTest
class ExcelExportMapperTest(
    private val excelExportMapper: ExcelExportMapper,
    private val seriesRepository: SeriesRegistrationRepository,
    private val supplierRepository: SupplierRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRegistrationRepository,
    private val productDTOMapper: ProductDTOMapper
) {
    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    private val email = "export@test.test"
    private val password = "export-123"
    private val supplierId: UUID = UUID.randomUUID()
    private val seriesId1: UUID = UUID.randomUUID()
    private val seriesId2: UUID = UUID.randomUUID()
    private val productId1: UUID = UUID.randomUUID()
    private val productId2: UUID = UUID.randomUUID()

    @Test
    fun testMapping() {
        val name1 = UUID.randomUUID().toString()
        runBlocking {
            supplierRepository.save(
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
            userRepository.createUser(
                User(
                    email = email, token = password, name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, supplierId.toString()))
                )
            )

            val testSeries1 =
                seriesRepository.save(
                    SeriesRegistration(
                        id = seriesId1,
                        draftStatus = DraftStatus.DONE,
                        supplierId = supplierId,
                        identifier = "apitest-series-123",
                        title = "apitest-series",
                        text = "apitest-series",
                        isoCategory = "12001314",
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

            val testSeries2 =
                seriesRepository.save(
                    SeriesRegistration(
                        id = seriesId2,
                        draftStatus = DraftStatus.DONE,
                        supplierId = supplierId,
                        identifier = "apitest-series-444",
                        title = "apitest-series444",
                        text = "apitest-series444",
                        isoCategory = "12001314",
                        status = SeriesStatus.ACTIVE,
                        adminStatus = AdminStatus.APPROVED,
                        seriesData =
                        SeriesDataDTO(
                            media = emptySet(),
                            attributes = SeriesAttributesDTO(keywords = listOf("keyword14", "keyword24")),
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

            val testProduct1 = productDTOMapper.toDTO(
                productRepository.save(
                    ProductRegistration(
                        seriesUUID = seriesId1,
                        title = "Dette er produkt 1",
                        articleName = "artikkelnavn",
                        id = productId1,
                        isoCategory = "04360901",
                        supplierId = supplierId,
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
                )
            )

            val testProduct2 = productDTOMapper.toDTO(
                productRepository.save(
                    ProductRegistration(
                        seriesUUID = seriesId2,
                        title = "Dette er produkt 2",
                        articleName = "artikkelnavn",
                        id = productId2,
                        isoCategory = "04360901",
                        supplierId = supplierId,
                        hmsArtNr = "11144",
                        supplierRef = "eksternref-11144",
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
                )
            )

            val productExcelExportDtos = excelExportMapper.mapToExportDtos(listOf(testProduct1, testProduct2))
            productExcelExportDtos shouldHaveSize 2
            productExcelExportDtos.first { it.seriesUuid == seriesId1.toString() }.let {
                it.seriesTitle shouldBe testSeries1.title
                it.seriesDescription shouldBe testSeries1.text

                it.products shouldHaveSize 1

                it.products.first().let { product ->
                    product.id shouldBe productId1
                }
            }

            productExcelExportDtos.first { it.seriesUuid == seriesId2.toString() }.let {
                it.seriesTitle shouldBe testSeries2.title
                it.seriesDescription shouldBe testSeries2.text

                it.products shouldHaveSize 1

                it.products.first().let { product ->
                    product.id shouldBe productId2
                }
            }
        }
    }
}