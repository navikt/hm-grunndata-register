package no.nav.hm.grunndata.register.product

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
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class ProductRegistrationApiTest(private val apiClient: ProductionRegistrationApiClient,
                                 private val loginClient: LoginClient,
                                 private val userRepository: UserRepository,
                                 private val supplierRepository: SupplierRepository) {

    val email = "api@test.test"
    val password = "api-123"
    val supplierId = UUID.randomUUID()
    val supplierId2 = UUID.randomUUID()
    var testSupplier: SupplierDTO? = null
    var testSupplier2: SupplierDTO? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            testSupplier = supplierRepository.save(
                Supplier(
                    id = supplierId,
                    info = SupplierInfo(
                        address = "address 3",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier3@test.test",
                    ),
                    identifier = "supplier3-unique-name",
                    name = "Supplier AS3",
                )
            ).toDTO()
            testSupplier2 = supplierRepository.save(
                Supplier(
                    id = supplierId2,
                    info = SupplierInfo(
                    address = "address 4",
                    homepage = "https://www.hompage.no",
                    phone = "+47 12345678",
                    email = "supplier4@test.test",
                ),
                identifier = "supplier4-unique-name",
                name = "Supplier AS4",
            )).toDTO()
            userRepository.createUser(
                User(
                    email = email, token = password, name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier!!.id.toString()))
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
            articleName = "Dette er produkt 1 med og med",
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
                MediaDTO(
                    uri = "123.jpg",
                    text = "bilde av produktet",
                    source = MediaSourceType.EXTERNALURL,
                    sourceUri = "https://ekstern.url/123.jpg"
                )
            ),
            agreementInfo = AgreementInfo(
                id = UUID.randomUUID(),
                identifier = "hmdbid-1",
                rank = 1,
                postNr = 1,
                reference = "AV-142",
                expired = LocalDateTime.now()
            ),
            createdBy = REGISTER,
            updatedBy = REGISTER
        )
        val registration = ProductRegistrationDTO(
            id = productDTO.id,
            supplierId = productDTO.supplier.id,
            supplierRef = productDTO.supplierRef,
            hmsArtNr = productDTO.hmsArtNr,
            title = productDTO.title,
            articleName = productDTO.articleName,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
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
        read.shouldNotBeNull()
        read.title shouldBe created.title
        read.articleName shouldBe created.articleName
        read.createdByUser shouldBe email

        val updated = apiClient.updateProduct(jwt, read.id, read.copy(title="new title", articleName = "new article title"))
        updated.shouldNotBeNull()
        updated.title shouldBe "new title"
        updated.articleName shouldBe "new article title"

        val deleted = apiClient.deleteProduct(jwt, updated.id)
        deleted.shouldNotBeNull()
        deleted.status shouldBe RegistrationStatus.DELETED
        deleted.productDTO.status shouldBe ProductStatus.INACTIVE

        val page = apiClient.findProducts(jwt,10,1,"created,asc")
        page.totalSize shouldBe 1

        val updatedVersion = apiClient.readProduct(jwt, updated.id)
        updatedVersion.version!! shouldBeGreaterThan 0
        updatedVersion.updatedByUser shouldBe email

        // should not be allowed to create a product of another supplier
        val productDTO2 = ProductDTO(
            id = UUID.randomUUID(),
            supplier = testSupplier2!!,
            title = "Dette er produkt 1",
            articleName = "Dette er produkt 1 med og med",
            attributes = mapOf(
                AttributeNames.shortdescription to "En kort beskrivelse av produktet",
                AttributeNames.text to "En lang beskrivelse av produktet"
            ),
            hmsArtNr = "111",
            identifier = "hmdb-222",
            supplierRef = "eksternref-222",
            isoCategory = "12001314",
            accessory = false,
            sparePart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(
                MediaDTO(
                    uri = "123.jpg",
                    text = "bilde av produktet",
                    source = MediaSourceType.EXTERNALURL,
                    sourceUri = "https://ekstern.url/123.jpg"
                )
            ),
            agreementInfo = AgreementInfo(
                id = UUID.randomUUID(),
                identifier = "hmdbid-1",
                rank = 1,
                postNr = 1,
                reference = "AV-142",
                expired = LocalDateTime.now()
            ),
            createdBy = REGISTER,
            updatedBy = REGISTER
        )
        val registration2 = ProductRegistrationDTO(
            id = productDTO2.id,
            supplierId = productDTO2.supplier.id,
            supplierRef = productDTO2.supplierRef,
            hmsArtNr = productDTO2.hmsArtNr,
            title = productDTO2.title,
            articleName = productDTO2.articleName,
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            status = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            createdByAdmin = false,
            expired = null,
            published = null,
            updatedByUser = email,
            createdByUser = email,
            productDTO = productDTO2,
            version = 1,
            createdBy = REGISTER,
            updatedBy = REGISTER
        )
        runCatching {
            val created2 = apiClient.createProduct(jwt, registration2)
        }.isFailure shouldBe true
    }

}
