package no.nav.hm.grunndata.register.product

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.AgreementDTO
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.rapid.dto.Attributes
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductAgreementStatus
import no.nav.hm.grunndata.rapid.dto.RegistrationStatus
import no.nav.hm.grunndata.rapid.dto.SeriesStatus
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.agreement.AgreementData
import no.nav.hm.grunndata.register.agreement.AgreementRegistration
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationRepository
import no.nav.hm.grunndata.register.agreement.DelkontraktData
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationDTO
import no.nav.hm.grunndata.register.agreement.DelkontraktRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementImportExcelService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistration
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.series.SeriesDataDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationDTO
import no.nav.hm.grunndata.register.series.SeriesRegistrationService
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
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
    private val seriesRegistrationService: SeriesRegistrationService
) {

    val email = "ProductRegistrationAdminApiTest@test.test"
    val password = "admin-123"
    val agreementId = UUID.randomUUID()
    val postId = UUID.randomUUID()
    val supplierId = UUID.randomUUID()
    val supplierRef = "eksternref-222"
    var testSupplier : SupplierRegistrationDTO? = null
    var productAgreement:  ProductAgreementRegistration? = null
    val seriesUUID: UUID = UUID.fromString("6e8830f0-1278-4031-a9b8-08389b55cf3e")

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)


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



            val agreement = AgreementDTO(id = agreementId, published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(2), title = "Title of agreement",
                text = "some text", reference = "unik-ref1", identifier = "unik-ref1", resume = "resume",
                posts = listOf(
                    AgreementPost(identifier = "unik-post1", title = "Post title",
                        description = "post description", nr = 1), AgreementPost(identifier = "unik-post2", title = "Post title 2",
                        description = "post description 2", nr = 2)
                ), createdBy = REGISTER, updatedBy = REGISTER,
                created = LocalDateTime.now(), updated = LocalDateTime.now())


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
                    AgreementPost(identifier = "unik-post1", title = "Post title", id = postId,
                        description = "post description", nr = 1),
                ))
            val agreementRegistration = AgreementRegistration(
                id = agreementId, published = agreement.published, expired = agreement.expired, title = agreement.title,
                reference = agreement.reference, updatedByUser = "username", createdByUser = "username", agreementData = data
            )

            agreementRegistrationRepository.save(agreementRegistration)



            val productAgreement = productAgreementRegistrationRepository.save(
                ProductAgreementRegistration(
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
                    articleName = "Test article"
                )
            )

            seriesRegistrationService.save(
                SeriesRegistrationDTO(
                    id = seriesUUID,
                    supplierId = supplierId,
                    isoCategory = "",
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
        draft.createdByAdmin shouldBe true
        draft.createdByUser shouldBe email

        // Edit the draft
        val productData = ProductDataDTO(
            attributes = Attributes(
                shortdescription = "En kort beskrivelse av produktet",
                text = "En lang beskrivelse av produktet"
            ),
            techData = listOf(
                TechDataDTO(
                    key = "maksvekt",
                    unit = "kg",
                    value = "120",
                    type = TechDataType.NUMBER,
                    definition = null,
                    options = null
                )
            ),
        )
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
        created.adminStatus shouldBe AdminStatus.PENDING
        created.registrationStatus shouldBe RegistrationStatus.ACTIVE

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
        deleted.registrationStatus shouldBe RegistrationStatus.DELETED

        val page = apiClient.findProducts(jwt = jwt,
            supplierId = supplierId, supplierRef = "eksternref-222",
            size = 20, page = 0, sort = "created,asc")
        page.totalSize shouldBe 1

        val updatedVersion = apiClient.readProduct(jwt, created.id)
        updatedVersion.version!! shouldBeGreaterThan 0




    }

}
