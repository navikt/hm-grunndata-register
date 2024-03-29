package no.nav.hm.grunndata.register.product

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class ProductRegistrationAdminApiTest(private val apiClient: ProductionRegistrationAdminApiClient,
                                      private val loginClient: LoginClient,
                                      private val userRepository: UserRepository,
                                      private val supplierRegistrationService: SupplierRegistrationService,
                                      private val objectMapper: ObjectMapper) {

    val email = "ProductRegistrationAdminApiTest@test.test"
    val password = "admin-123"
    val supplierId = UUID.randomUUID()
    var testSupplier : SupplierRegistrationDTO? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @MockBean(GdbApiClient::class)
    fun mockGdbApiClient(): GdbApiClient = mockk(relaxed = true)

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
        }
    }

    @Test
    fun aGoodDayRegistrationScenarioTest() {
        // Login to get authentication cookie
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value

        // create a draft to begin product registration
        val draft = apiClient.draftProduct(jwt, testSupplier!!.id)
        draft.shouldNotBeNull()
        draft.createdByAdmin shouldBe true
        draft.createdByUser shouldBe email
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(draft))

        // Edit the draft
        val productData = draft.productData.copy(
            attributes = Attributes(
                shortdescription = "En kort beskrivelse av produktet",
                text = "En lang beskrivelse av produktet"
            ),
            accessory = false,
            sparePart = false,
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = setOf(
                MediaInfo(
                    uri = "123.jpg",
                    text = "bilde av produktet",
                    source = MediaSourceType.EXTERNALURL,
                    sourceUri = "https://ekstern.url/123.jpg"
                )
            ),
        )
        val hmsArtNr = UUID.randomUUID().toString()
        val registration = draft.copy(
            seriesId = "series-123",
            isoCategory = "12001314",
            hmsArtNr = hmsArtNr,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            message = "Melding til leverandør",
            adminInfo = null,
            productData = productData
        )

        // update draft
        val created = apiClient.updateProduct(jwt, registration.id, registration)
        created.shouldNotBeNull()
        created.adminStatus shouldBe AdminStatus.PENDING
        created.registrationStatus shouldBe RegistrationStatus.ACTIVE

        // read it from database
        val read = apiClient.readProduct(jwt, created.id)
        read.shouldNotBeNull()
        read.createdByUser shouldBe email
        read.hmsArtNr shouldBe hmsArtNr

        // make some changes, with approved by admin
        val updated = apiClient.updateProduct(jwt, read.id, read.copy(title = "Changed title", supplierRef = "eksternref-222",
            draftStatus = DraftStatus.DONE, registrationStatus = RegistrationStatus.ACTIVE))

        updated.shouldNotBeNull()
        updated.title shouldBe "Changed title"

        // approve the product
        val approved = apiClient.approveProduct(jwt, updated.id)
        approved.shouldNotBeNull()
        approved.draftStatus shouldBe DraftStatus.DONE
        approved.adminStatus shouldBe AdminStatus.APPROVED
        approved.adminInfo.shouldNotBeNull()
        approved.adminInfo?.approvedBy shouldBe  email


        val draftStatusChange = apiClient.updateProduct(jwt, approved.id, approved.copy(draftStatus = DraftStatus.DRAFT))
        draftStatusChange.shouldNotBeNull()
        draftStatusChange.draftStatus shouldBe DraftStatus.DONE

        // flag the registration to deleted
        val deleted = apiClient.deleteProduct(jwt, updated.id)
        deleted.shouldNotBeNull()
        deleted.registrationStatus shouldBe RegistrationStatus.DELETED

        val page = apiClient.findProducts(jwt = jwt,
            supplierId = supplierId, supplierRef = "eksternref-222",
            size = 20, page = 0, sort = "created,asc")
        page.totalSize shouldBe 1

        val updatedVersion = apiClient.readProduct(jwt, updated.id)
        updatedVersion.version!! shouldBeGreaterThan 0
        updatedVersion.updatedByUser shouldBe email

    }

}
