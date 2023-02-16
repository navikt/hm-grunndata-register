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
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.Supplier
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.supplier.toDTO
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductRegistrationAdminApiTest(private val apiClient: ProductionRegistrationAdminApiClient,
                                      private val loginClient: LoginClient,
                                      private val userRepository: UserRepository,
                                      private val supplierRepository: SupplierRepository,
                                      private val objectMapper: ObjectMapper) {

    val email = "admin@test.test"
    val password = "admin-123"
    val supplierId = UUID.randomUUID()
    var testSupplier : SupplierDTO? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            testSupplier = supplierRepository.save(
                Supplier(
                    id = supplierId,
                    info = SupplierInfo(
                        address = "address 4",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier4@test.test",
                    ),
                    identifier = "supplier4-unique-name",
                    name = "Supplier AS4",
                )
            ).toDTO()
            userRepository.createUser(
                User(
                    email = email, token = password, name = "User tester", roles = listOf(Roles.ROLE_ADMIN)
                )
            )
        }
    }

    @Test
    fun apiTest() {
        val resp = loginClient.login(UsernamePasswordCredentials(email, password))
        val jwt = resp.getCookie("JWT").get().value
        val productDTO = ProductDTO(
            id = UUID.randomUUID(),
            supplier = testSupplier!!,
            title = "Dette er produkt 1",
            attributes = mapOf(
                AttributeNames.articlename to "produktnavn", AttributeNames.shortdescription to "En kort beskrivelse av produktet",
                AttributeNames.text to "En lang beskrivelse av produktet"
            ),
            hmsArtNr = "111",
            identifier = "hmdb-111",
            supplierRef = "eksternref-111",
            isoCategory = "12001314",
            accessory = false,
            sparePart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(
                Media(
                    uri = "https://ekstern.url/123.jpg",
                    text = "bilde av produktet",
                    source = MediaSourceType.EXTERNALURL
                )
            ),
            agreementInfo = AgreementInfo(
                id = UUID.randomUUID(),
                identifier = "hmdbid-1",
                rank = 1,
                postNr = 1,
                reference = "AV-142",
                expired =  LocalDateTime.now()
            ),
            createdBy = REGISTER,
            updatedBy = REGISTER
        )
        val registration = ProductRegistrationDTO(
            id = productDTO.id,
            supplierId = productDTO.supplier.id,
            supplierRef = productDTO.supplierRef,
            HMSArtNr = productDTO.hmsArtNr,
            title = productDTO.title,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.NOT_APPROVED,
            status = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            createdByAdmin = false,
            expired = null,
            published = null,
            updatedByUser = email,
            createdByUser = email,
            productDTO = productDTO,
            version = 1,
            createdBy = REGISTER,
            updatedBy = REGISTER
        )
        val created = apiClient.createProduct(jwt, registration)
        created.shouldNotBeNull()

        val read = apiClient.readProduct(jwt, created.id)
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(read))
        read.shouldNotBeNull()
        read.title shouldBe created.title
        read.createdByUser shouldBe email

        val updated = apiClient.updateProduct(jwt, read.id, read.copy(title="new title"))
        updated.shouldNotBeNull()
        updated.title shouldBe "new title"

        val deleted = apiClient.deleteProduct(jwt, updated.id)
        deleted.shouldNotBeNull()
        deleted.status shouldBe RegistrationStatus.DELETED
        deleted.productDTO.status shouldBe ProductStatus.INACTIVE

        val page = apiClient.findProducts(jwt = jwt,
            supplierId = supplierId, supplierRef = "eksternref-111",
            size = 20, page = 0, sort = "created,asc")
        page.totalSize shouldBe 1

        val updatedVersion = apiClient.readProduct(jwt, updated.id)
        updatedVersion.version!! shouldBeGreaterThan 0
        updatedVersion.updatedByUser shouldBe email
    }

}