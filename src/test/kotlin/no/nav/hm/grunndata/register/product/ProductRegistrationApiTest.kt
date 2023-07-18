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
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.grunndata.register.REGISTER
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.*
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
                                 private val supplierService: SupplierService) {

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
        val name1 = UUID.randomUUID().toString()
        val name2 = UUID.randomUUID().toString()
        runBlocking {
            testSupplier = supplierService.save(
                SupplierRegistrationDTO(
                    id = supplierId,
                    supplierData = SupplierData(
                        address = "address 3",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier3@test.test",
                    ),
                    identifier =  name1,
                    name =  name1,
                )
            ).toRapidDTO()
            testSupplier2 = supplierService.save(
                SupplierRegistrationDTO(
                    id = supplierId2,
                    supplierData = SupplierData(
                    address = "address 4",
                    homepage = "https://www.hompage.no",
                    phone = "+47 12345678",
                    email = "supplier4@test.test",
                ),
                identifier = name2,
                name = name2
            )).toRapidDTO()
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
        val productData = ProductData(
            attributes = Attributes(
                shortdescription = "En kort beskrivelse av produktet",
                text = "En lang beskrivelse av produktet"
            ),
            isoCategory = "12001314",
            accessory = false,
            sparePart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(
                MediaInfo(
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
            )
        )

        val registration = ProductRegistrationDTO(
            title = "Dette er produkt 1",
            articleName = "Dette er produkt 1 med og med",
            id = UUID.randomUUID(),
            supplierId = testSupplier!!.id,
            hmsArtNr = "111",
            supplierRef = "eksternref-111",
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            createdByAdmin = false,
            expired = null,
            published = null,
            updatedByUser = email,
            createdByUser = email,
            productData = productData,
            version = 1,
            createdBy = REGISTER,
            updatedBy = REGISTER
        )
        val created = apiClient.createProduct(jwt, registration)
        created.shouldNotBeNull()

        // create another one

        val productData2 = ProductData(
            attributes = Attributes(
                shortdescription = "En kort beskrivelse av produktet",
                text = "En lang beskrivelse av produktet"
            ),
            isoCategory = "12001314",
            accessory = false,
            sparePart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(
                MediaInfo(
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
            )
        )

        val registration2 = ProductRegistrationDTO(
            title = "en veldig fin tittel",
            articleName = "en veldig fin tittel med og med",
            id = UUID.randomUUID(),
            supplierId = testSupplier!!.id,
            hmsArtNr = "222",
            supplierRef = "eksternref-222",
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            createdByAdmin = false,
            expired = null,
            published = null,
            updatedByUser = email,
            createdByUser = email,
            productData = productData2,
            version = 1,
            createdBy = REGISTER,
            updatedBy = REGISTER
        )

        val created2 = apiClient.createProduct(jwt, registration2)
        created2.shouldNotBeNull()

        val read = apiClient.readProduct(jwt, created.id)
        read.shouldNotBeNull()
        read.createdByUser shouldBe email

        val updated = apiClient.updateProduct(jwt, read.id, read.copy(title="Changed title", articleName = "Changed articlename"))
        updated.shouldNotBeNull()


        val deleted = apiClient.deleteProduct(jwt, updated.id)
        deleted.shouldNotBeNull()
        deleted.registrationStatus shouldBe RegistrationStatus.DELETED

        val page = apiClient.findProducts(jwt,null, null, 20,1,"created,asc")
        page.totalSize shouldBe 2

        val page2 = apiClient.findProducts(jwt,"222", null, 30,1,"created,asc")
        page2.totalSize shouldBe 1

        val page3 = apiClient.findProducts(jwt,null, "%en veldig%", 30,1,"created,asc")
        page3.totalSize shouldBe 1

        val updatedVersion = apiClient.readProduct(jwt, updated.id)
        updatedVersion.version!! shouldBeGreaterThan 0
        updatedVersion.updatedByUser shouldBe email

        // should not be allowed to create a product of another supplier
        val productData3 = ProductData (
            attributes = Attributes (
                shortdescription = "En kort beskrivelse av produktet",
                text = "En lang beskrivelse av produktet"
            ),
            isoCategory = "12001314",
            accessory = false,
            sparePart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(
                MediaInfo(
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
            )
        )
        val registration3 = ProductRegistrationDTO(
            id = UUID.randomUUID(),
            supplierId = testSupplier2!!.id,
            title = "Dette er produkt 1",
            articleName = "Dette er produkt 1 med og med",
            hmsArtNr = "333",
            supplierRef = "eksternref-333",
            draftStatus = DraftStatus.DRAFT,
            adminStatus = AdminStatus.PENDING,
            registrationStatus = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            createdByAdmin = false,
            expired = null,
            published = null,
            updatedByUser = email,
            createdByUser = email,
            productData = productData3,
            version = 1,
            createdBy = REGISTER,
            updatedBy = REGISTER
        )
        runCatching {
            val created3 = apiClient.createProduct(jwt, registration3)
        }.isFailure shouldBe true

    }

}
