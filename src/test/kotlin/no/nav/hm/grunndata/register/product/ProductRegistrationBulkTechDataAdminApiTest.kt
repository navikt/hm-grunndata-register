package no.nav.hm.grunndata.register.product

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistration
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.techlabel.LabelService
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAdminApiClient
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRegistrationDTO
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

// Kept in its own test class (rather than as an extra @Test in ProductRegistrationAdminApiTest)
// because that class's @BeforeEach seeds fixed-id fixtures (supplier/series/agreement) which are
// not safe to insert twice against the same database - every extra @Test in that class makes
// @BeforeEach run again and collide on unique constraints.
@MicronautTest
class ProductRegistrationBulkTechDataAdminApiTest(
    private val apiClient: ProductRegistrationAdminApiClient,
    private val loginClient: LoginClient,
    private val userRepository: UserRepository,
    private val userAdminApiClient: UserAdminApiClient,
    private val supplierRegistrationService: SupplierRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val objectMapper: ObjectMapper,
) {

    val email = "ProductRegistrationBulkTechDataAdminApiTest@test.test"
    val password = "admin-123"
    val supplierId = UUID.randomUUID()
    val seriesUUID = UUID.randomUUID()

    val techlabeljson = """
        [
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
          }
        ]
    """.trimIndent()

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @MockBean(LabelService::class)
    fun mockTechLabelService(): LabelService = mockk<LabelService>().apply {
        every {
            fetchLabelsByIsoCode("04360901")
        } returns objectMapper.readValue(techlabeljson, object : TypeReference<List<TechLabelDTO>>() {})
            .sortedBy { it.sort }
    }

    @BeforeEach
    fun createUserAndSupplier() {
        runBlocking {
            supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = supplierId,
                    supplierData = SupplierData(
                        address = "address bulk-tech-data",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier-bulk-tech-data@test.test",
                    ),
                    identifier = "supplier-bulk-tech-data-unique-name",
                    name = "Supplier Bulk Tech Data AS",
                )
            )
            userRepository.createUser(
                User(
                    email = email, token = password, name = "Bulk tech data tester", roles = listOf(Roles.ROLE_ADMIN)
                )
            )
            seriesRegistrationService.save(
                SeriesRegistration(
                    id = seriesUUID,
                    supplierId = supplierId,
                    isoCategory = "04360901",
                    title = "",
                    text = "",
                    identifier = seriesUUID.toString(),
                    draftStatus = DraftStatus.DRAFT,
                    adminStatus = AdminStatus.PENDING,
                    status = SeriesStatus.ACTIVE,
                    createdBy = REGISTER,
                    updatedBy = REGISTER,
                    createdByUser = "",
                    updatedByUser = "authentication.name",
                    created = LocalDateTime.now(),
                    updated = LocalDateTime.now(),
                    seriesData = SeriesDataDTO(media = emptySet()),
                    version = 0,
                )
            )
        }
    }

    @Test
    fun bulkUpdateTechDataTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value

        val draft1 = apiClient.createDraft(jwt, seriesUUID, DraftVariantDTO("bulk-1", "bulk-1"))
        val draft2 = apiClient.createDraft(jwt, seriesUUID, DraftVariantDTO("bulk-2", "bulk-2"))

        val techData1 = ExtendedTechDataDTO(
            key = "Vekt", unit = "kg", value = "10", type = TechDataType.NUMBER,
            definition = null, required = false, options = emptySet(),
        )
        val techData2 = ExtendedTechDataDTO(
            key = "Vekt", unit = "kg", value = "20", type = TechDataType.NUMBER,
            definition = null, required = false, options = emptySet(),
        )

        // happy path: two variants updated in one bulk call
        val result = apiClient.bulkUpdateTechData(
            jwt,
            BulkTechDataUpdateDTO(
                updates = listOf(
                    VariantTechDataUpdate(draft1.id, listOf(techData1)),
                    VariantTechDataUpdate(draft2.id, listOf(techData2)),
                )
            )
        )
        result.failed.shouldBeEmpty()
        result.updated.size shouldBe 2
        result.updated.find { it.id == draft1.id }!!.productData.techData shouldContain techData1
        result.updated.find { it.id == draft2.id }!!.productData.techData shouldContain techData2

        // unauthorized: a non-admin user tied to a different supplier cannot bulk update this variant.
        // ROLE_HMS passes the controller-level @Secured check but is rejected by the per-variant
        // supplier ownership check inside bulkUpdateTechData. The user is created via the admin
        // HTTP API (not a direct repository call) so the write is committed and visible to the
        // separate connection handling the login call below.
        val otherSupplierId = UUID.randomUUID()
        val otherEmail = "other-supplier-bulk-tech-data@test.test"
        userAdminApiClient.createUser(
            jwt,
            UserRegistrationDTO(
                name = "Other supplier user",
                email = otherEmail,
                password = password,
                roles = listOf(Roles.ROLE_HMS),
                attributes = mapOf(UserAttribute.SUPPLIER_ID to otherSupplierId.toString()),
            )
        )
        val otherResp = loginClient.login(UsernamePasswordCredentials(otherEmail, password))
        val otherJwt = otherResp.getCookie("JWT").get().value

        val unauthorizedResult = apiClient.bulkUpdateTechData(
            otherJwt,
            BulkTechDataUpdateDTO(updates = listOf(VariantTechDataUpdate(draft1.id, listOf(techData2))))
        )
        unauthorizedResult.updated.shouldBeEmpty()
        unauthorizedResult.failed.size shouldBe 1
        unauthorizedResult.failed[0].productId shouldBe draft1.id

        // the rejected update must not have overwritten the value set in the happy-path call above
        val stillOriginal = apiClient.readProduct(jwt, draft1.id)
        stillOriginal.productData.techData shouldContain techData1
    }
}
