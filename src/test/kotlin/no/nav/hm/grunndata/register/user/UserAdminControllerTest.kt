package no.nav.hm.grunndata.register.user

import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.SupplierInfo
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.Supplier
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.supplier.toDTO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class UserAdminControllerTest(private val userRepository: UserRepository,
                              private val loginClient: LoginClient,
                              private val userAdminApiClient: UserAdminApiClient,
                              private val supplierRepository: SupplierRepository) {

    val adminEmail = "admin@test.test"
    val password = "test123"

    val userEmail = "user@test.test"

    val supplierId = UUID.randomUUID()

    private var testSupplier: SupplierDTO? = null

    @BeforeEach
    fun createAdminUser() {
        runBlocking {
            testSupplier = supplierRepository.save(
                Supplier(
                    id = supplierId,
                    info = SupplierInfo(
                        address = "address 4",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier5@test.test",
                    ),
                    identifier = "supplier5-unique-name",
                    name = "Supplier AS5",
                )
            ).toDTO()
            userRepository.createUser(
                User(
                    email = adminEmail, token = password, name = "Admin tester", roles = listOf(Roles.ROLE_ADMIN)
                )
            )
            userRepository.createUser(
                User(
                    email = userEmail, token = password, name = "User test", roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier!!.id.toString()))
                )
            )
        }
    }

    @Test
    fun adminUserTest() {
        val jwtAdmin = loginClient.login(UsernamePasswordCredentials(adminEmail,  password))
            .getCookie("JWT").get().value
        val jwtUser = loginClient.login(UsernamePasswordCredentials(userEmail, password))
            .getCookie("JWT").get().value

        // admin user can create new user
        val resp = userAdminApiClient.createUser(jwtAdmin, UserRegistrationDTO (
            email = "anotheruser@test.test", password = "test123", roles = listOf(Roles.ROLE_SUPPLIER),
            name = "another user", attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier!!.id.toString()))
        ))
        resp.status shouldBe  HttpStatus.CREATED

        // supplier can not create new user
        // TODO need to check if we should allow supplier to create new user.
        runCatching {
            val resp2 = userAdminApiClient.createUser(jwtUser, UserRegistrationDTO (
                email = "anotheruser2@test.test", password = "test123", roles = listOf(Roles.ROLE_SUPPLIER),
                name = "another user2", attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier!!.id.toString()))
            ))
        }.isFailure shouldBe true

        //
    }

}
