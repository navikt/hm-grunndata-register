package no.nav.hm.grunndata.register.product

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.Supplier
import no.nav.hm.grunndata.register.supplier.SupplierInfo
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class ProductRegistrationAdminApiTest(private val apiClient: ProductionRegistrationAdminApiClient,
                                      private val loginClient: LoginClient, private val userRepository: UserRepository,
                                      private val supplierRepository: SupplierRepository) {

    val email = "admin@test.test"
    val password = "admin-123"
    val supplierId = UUID.randomUUID()

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplier = supplierRepository.save(
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
            )
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
            supplierId = supplierId,
            title = "Dette er produkt 1",
            description = Description(
                "produktnavn", "En kort beskrivelse av produktet",
                "En lang beskrivelse av produktet"
            ),
            HMSArtNr = "111",
            identifier = "hmdb-111",
            supplierRef = "eksternref-111",
            isoCategory = "12001314",
            accessory = false,
            sparepart = false,
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
                id = 1,
                identifier = "hmdbid-1",
                rank = 1,
                postId = 123,
                postNr = 1,
                reference = "AV-142"
            )
        )
        val registration = ProductRegistrationDTO(
            id = productDTO.id,
            supplierId = productDTO.supplierId,
            supplierRef = productDTO.supplierRef,
            HMSArtNr = productDTO.HMSArtNr,
            title = productDTO.title,
            draft = DraftStatus.DRAFT,
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
            version = 1
        )
        val created = apiClient.createProduct(jwt, registration)
        created.shouldNotBeNull()

        val read = apiClient.readProduct(jwt, created.id)
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
            size = 20, number = 0, sort = "created,asc")
        page.totalSize shouldBe 1

        val updatedVersion = apiClient.readProduct(jwt, updated.id)
        updatedVersion.version!! shouldBeGreaterThan 0
        updatedVersion.updatedByUser shouldBe email
    }

}