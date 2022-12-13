package no.nav.hm.grunndata.register.product

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.cookie.Cookie
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.Supplier
import no.nav.hm.grunndata.register.supplier.SupplierInfo
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.ws.rs.core.MediaType

@MicronautTest
class ProductRegistrationApiControllerTest(private val userRepository: UserRepository,
                                           private val supplierRepository: SupplierRepository,
                                           @Client("/") private val client: HttpClient) {

    val email = "user3@test.test"
    val token = "token-123"
    val supplierId = UUID.randomUUID()

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplier = supplierRepository.save(
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
            )
            userRepository.createUser(
                User(
                    email = email, token = token, name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier.id.toString()))
                )
            )
        }
    }

    @Test
    fun crudProductRegistration() {
        val jwt = getLoginCookie(client, email, token)
        jwt.shouldNotBeNull()

        val productDTO = ProductDTO (
            supplierId = supplierId,
            title = "Dette er produkt 1",
            description = Description("produktnavn", "En kort beskrivelse av produktet",
                "En lang beskrivelse av produktet"),
            HMSArtNr = "111",
            identifier = "hmdb-111",
            supplierRef = "eksternref-111",
            isoCategory = "12001314",
            accessory = false,
            sparepart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(Media(uri="https://ekstern.url/123.jpg", text = "bilde av produktet", source = MediaSourceType.EXTERNALURL)),
            agreementInfo = AgreementInfo(id = 1, identifier = "hmdbid-1", rank = 1, postId = 123, postNr = 1, reference = "AV-142")
        )
        val registration = ProductRegistration (
            id = productDTO.id,
            supplierId = productDTO.supplierId,
            supplierRef = productDTO.supplierRef,
            HMSArtNr = productDTO.HMSArtNr ,
            title = productDTO.title,
            draft = DraftStatus.DRAFT,
            adminStatus = AdminStatus.NOT_APPROVED,
            status  = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            productDTO = productDTO
        )
        // create
        var respons = client.toBlocking().exchange(
            HttpRequest.POST(ProductRegistrationApi.API_V1_PRODUCT_REGISTRATIONS, registration)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), ProductRegistrationDTO::class.java
        )
        respons.status() shouldBe HttpStatus.CREATED

        // update
        val updateDTO = productDTO.copy(title = "Ny title")
        val updatedReg = registration.copy(title = "Ny title", productDTO = updateDTO)
        respons = client.toBlocking().exchange(
            HttpRequest.PUT("${ProductRegistrationApi.API_V1_PRODUCT_REGISTRATIONS}/${updateDTO.id}", updatedReg)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), ProductRegistrationDTO::class.java
        )

        respons.status shouldBe HttpStatus.OK

        // read
        respons = client.toBlocking().exchange(
            HttpRequest.GET<ProductRegistrationDTO>("${ProductRegistrationApi.API_V1_PRODUCT_REGISTRATIONS}/${updateDTO.id}")
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), ProductRegistrationDTO::class.java
        )

        respons.status shouldBe HttpStatus.OK
        respons.body().shouldNotBeNull()

        // Delete
        respons = client.toBlocking().exchange(
            HttpRequest.DELETE<ProductRegistrationDTO>("${ProductRegistrationApi.API_V1_PRODUCT_REGISTRATIONS}/${updateDTO.id}")
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), ProductRegistrationDTO::class.java
        )
        respons.status shouldBe HttpStatus.OK
        val deleted = respons.body()
        deleted.shouldNotBeNull()
        deleted.status shouldBe RegistrationStatus.DELETED
        deleted.productDTO.status shouldBe ProductStatus.INACTIVE



        val productDTO2 = ProductDTO (
            supplierId = supplierId,
            title = "Dette er produkt 2",
            description = Description("produktnavn", "En kort beskrivelse av produktet",
                "En lang beskrivelse av produktet"),
            HMSArtNr = "222",
            identifier = "hmdb-222",
            supplierRef = "eksternref-222",
            isoCategory = "12001314",
            accessory = false,
            sparepart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(Media(uri="https://ekstern.url/222.jpg", text = "bilde av produktet", source = MediaSourceType.EXTERNALURL)),
            agreementInfo = AgreementInfo(id = 1, identifier = "hmdbid-1", rank = 1, postId = 123, postNr = 1, reference = "AV-142")
        )
        val registration2 = ProductRegistration (
            id = productDTO2.id,
            supplierId = productDTO2.supplierId,
            supplierRef = productDTO2.supplierRef,
            HMSArtNr = productDTO2.HMSArtNr ,
            title = productDTO2.title,
            draft = DraftStatus.DRAFT,
            adminStatus = AdminStatus.NOT_APPROVED,
            status  = RegistrationStatus.ACTIVE,
            message = "Melding til leverandør",
            adminInfo = null,
            productDTO = productDTO2
        )
        // create 2
        client.toBlocking().exchange(
            HttpRequest.POST(ProductRegistrationApi.API_V1_PRODUCT_REGISTRATIONS, registration2)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), ProductRegistrationDTO::class.java
        )

        // list products
        respons = client.toBlocking().exchange(
            HttpRequest.GET<ProductRegistrationDTO>(ProductRegistrationApi.API_V1_PRODUCT_REGISTRATIONS)
                .accept(MediaType.APPLICATION_JSON)
                .cookie(jwt), ProductRegistrationDTO::class.java
        )
    }

}

fun getLoginCookie(client: HttpClient, email: String, token: String): Cookie {
    val request = HttpRequest.POST<Any>("/login", UsernamePasswordCredentials(email, token))
    val resp = client.toBlocking().exchange<Any, Any>(request)
    return resp.getCookie("JWT").get()
}