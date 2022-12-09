package no.nav.hm.grunndata.register.product

import io.kotest.matchers.nulls.shouldNotBeNull
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.cookie.Cookie
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.Supplier
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.user.User
import no.nav.hm.grunndata.register.user.UserAttribute
import no.nav.hm.grunndata.register.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class ProductRegistrationApiControllerTest(private val userRepository: UserRepository,
                                           private val supplierRepository: SupplierRepository,
                                           @Client("/") private val client: HttpClient) {

    val email = "user3@test.test"
    val token = "token-123"

    @BeforeEach
    fun createUserSupplier() {
        runBlocking {
            val testSupplier = supplierRepository.save(
                Supplier(
                    email = "supplier3@test.test",
                    identifier = "supplier3-unique-name",
                    name = "Supplier AS3",
                    address = "address 3",
                    homepage = "https://www.hompage.no",
                    phone = "+47 12345678"
                )
            )
            userRepository.createUser(
                User(
                    email = email, token = token, name = "User tester", roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier.id))
                )
            )
        }
    }

    @Test
    fun crudProductRegistration() {
        val jwt = getLoginCookie(client, email, token)
        jwt.shouldNotBeNull()
        val productDTO = ProductDTO (
            supplierId = UUID.randomUUID(),
            title = "Dette er produkt title",
            description = Description("produktnavn", "En kort beskrivelse av produktet",
                "En lang beskrivelse av produktet"),
            HMSArtNr = "123",
            identifier = "hmdb-123",
            supplierRef = "eksternref-123",
            isoCategory = "12001314",
            accessory = false,
            sparepart = false,
            seriesId = "series-123",
            techData = listOf(TechData(key = "maksvekt", unit = "kg", value = "120")),
            media = listOf(Media(uri="https://ekstern.url/123.jpg", text = "bilde av produktet", source = MediaSourceType.EXTERNALURL)),
            agreementInfo = AgreementInfo(id = 1, identifier = "hmdbid-1", rank = 1, postId = 123, postNr = 1, reference = "AV-142")
        )
        val registration = ProductRegistration (
            supplierId = UUID.randomUUID(),
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
    }

}

fun getLoginCookie(client: HttpClient, email: String, token: String): Cookie {
    val request = HttpRequest.POST<Any>("/login", UsernamePasswordCredentials(email, token))
    val resp = client.toBlocking().exchange<Any, Any>(request)
    return resp.getCookie("JWT").get()
}