package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementData
import no.nav.hm.grunndata.register.agreement.AgreementRegistration
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationRepository
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationService
import no.nav.hm.grunndata.register.catalog.ProductAgreementImportExcelService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementAdminClient
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationDTO
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
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
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@MicronautTest
class ProductRegistrationAdminApiTest(
    private val apiClient: ProductRegistrationAdminApiClient,
    private val loginClient: LoginClient,
    private val userRepository: UserRepository,
    private val supplierRegistrationService: SupplierRegistrationService,
    private val agreementRegistrationRepository: AgreementRegistrationRepository,
    private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
    private val delkontraktRegistrationService: DelkontraktRegistrationService,
    private val seriesRegistrationService: SeriesRegistrationService,
    private val productAgreementAdminClient: ProductAgreementAdminClient,
    private val objectMapper: ObjectMapper
) {

    val email = "ProductRegistrationAdminApiTest@test.test"
    val password = "admin-123"
    val agreementId = UUID.randomUUID()
    val postId = UUID.randomUUID()
    val supplierId = UUID.randomUUID()
    val supplierRef = "eksternref-222"
    var testSupplier: SupplierRegistrationDTO? = null
    val seriesUUID: UUID = UUID.fromString("6e8830f0-1278-4031-a9b8-08389b55cf3e")

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

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @MockBean(LabelService::class)
    fun mockTechLabelService(): LabelService = mockk<LabelService>().apply {
        every {
            fetchLabelsByIsoCode("04360901")
        } returns
                objectMapper.readValue(techlabeljson, object : TypeReference<List<TechLabelDTO>>() {})
                    .sortedBy { it.sort }
    }

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            testSupplier = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = supplierId,
                    supplierData = SupplierData(
                        address = "address 4",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier4@test.test",
                    ),
                    identifier = "supplier4-unique-name",
                    name = "Supplier AS4",
                )
            )
            userRepository.createUser(
                User(
                    email = email, token = password, name = "User tester", roles = listOf(Roles.ROLE_ADMIN)
                )
            )


            val agreement = AgreementDTO(
                id = agreementId, published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(2), title = "Title of agreement",
                text = "some text", reference = "unik-ref1", identifier = "unik-ref1", resume = "resume",
                posts = listOf(
                    AgreementPost(
                        identifier = "unik-post1", title = "Post title",
                        description = "post description", nr = 1
                    ), AgreementPost(
                        identifier = "unik-post2", title = "Post title 2",
                        description = "post description 2", nr = 2
                    )
                ), createdBy = REGISTER, updatedBy = REGISTER,
                created = LocalDateTime.now(), updated = LocalDateTime.now()
            )


            val delkontraktToSave = DelkontraktRegistrationDTO(
                id = postId,
                agreementId = agreement.id,
                delkontraktData = DelkontraktData(title = "delkontrakt 1", description = "beskrivelse", sortNr = 1),
                createdBy = "tester",
                updatedBy = "tester",
                identifier = postId.toString()
            )

            delkontraktRegistrationService.save(delkontraktToSave)

            val data = AgreementData(
                text = "some text", resume = "resume",
                identifier = agreement.identifier,
                posts = listOf(
                    AgreementPost(
                        identifier = "unik-post1", title = "Post title", id = postId,
                        description = "post description", nr = 1
                    ),
                )
            )
            val agreementRegistration = AgreementRegistration(
                id = agreementId,
                published = agreement.published,
                expired = agreement.expired,
                title = agreement.title,
                reference = agreement.reference,
                updatedByUser = "username",
                createdByUser = "username",
                agreementData = data
            )

            agreementRegistrationRepository.save(agreementRegistration)



            val series = seriesRegistrationService.save(
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
    fun aGoodDayRegistrationScenarioTest() {
        // Login to get authentication cookie
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value

        // create a draft to begin product registration
        val draft = apiClient.createDraft(jwt, seriesUUID, DraftVariantDTO("test", "test"))
        draft.shouldNotBeNull()

        // Edit the draft
        val extendedTechData = ExtendedTechDataDTO(
            key = "Vekt",
            unit = "kg",
            value = "120",
            type = TechDataType.NUMBER,
            definition = null,
            options = emptyList()
        )
        val productData = ProductDataDTO(techData = listOf(extendedTechData))
        val hmsArtNr = UUID.randomUUID().toString()

        val updateDTO = UpdateProductRegistrationDTO(
            articleName = draft.articleName,
            supplierRef = supplierRef,
            hmsArtNr = hmsArtNr,
            productData = productData
        )

        // update draft
        val created = apiClient.updateProduct(jwt, draft.id, updateDTO)
        created.shouldNotBeNull()
        created.productData.techData shouldContain extendedTechData

        productAgreementAdminClient.createProductAgreement(jwt,
            ProductAgreementRegistrationDTO(
                agreementId = agreementId,
                hmsArtNr = "12345",
                post = 1,
                rank = 1,
                postId = postId,
                reference = "20-1423",
                supplierId = supplierId,
                supplierRef = supplierRef,
                createdBy = ProductAgreementImportExcelService.EXCEL,
                title = "Test product agreement",
                status = ProductAgreementStatus.ACTIVE,
                articleName = "Test article",
                productId = created.id,
                seriesUuid = seriesUUID,
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(1)
            ))

        // read it from database
        val read = apiClient.readProduct(jwt, created.id)
        read.shouldNotBeNull()
        read.hmsArtNr shouldBe hmsArtNr
        read.agreements.size shouldBe 1
        read.agreements[0].id shouldBe agreementId
        read.agreements[0].postId shouldBe postId
        read.agreements[0].postNr shouldBe 1

        // flag the registration to deleted
        val deleted = apiClient.deleteProduct(jwt, listOf(created.id))
        deleted.shouldNotBeNull()

        val page = apiClient.findProducts(
            jwt = jwt,
            supplierId = supplierId, supplierRef = "eksternref-222",
            size = 20, page = 0, sort = "created,asc"
        )
        page.totalSize shouldBe 1

        val updatedVersion = apiClient.readProduct(jwt, created.id)
        updatedVersion.version!! shouldBeGreaterThan 0


    }

}
