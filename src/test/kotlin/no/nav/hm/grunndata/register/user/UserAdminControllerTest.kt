package no.nav.hm.grunndata.register.user

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpStatus
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.grunndata.rapid.event.RapidApp
import no.nav.hm.grunndata.register.security.LoginClient
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class UserAdminControllerTest(private val userRepository: UserRepository,
                              private val loginClient: LoginClient,
                              private val userAdminApiClient: UserAdminApiClient,
                              private val supplierRegistrationService: SupplierRegistrationService) {

    val adminEmail = "randomadmin@test.test"
    val password = "test123"

    val userId = UUID.randomUUID()
    val userEmail = "randomuser@test.test"

    val supplierId = UUID.randomUUID()

    private var testSupplier: SupplierRegistrationDTO? = null

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @BeforeEach
    fun createAdminUser() {
        runBlocking {
            testSupplier = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = supplierId,
                    supplierData = SupplierData (
                        address = "address 4",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678",
                        email = "supplier5@test.test",
                    ),
                    status = SupplierStatus.ACTIVE,
                    createdByUser = "admin",
                    updatedByUser = "admin",
                    createdBy = RapidApp.grunndata_register,
                    updatedBy = RapidApp.grunndata_register,
                    identifier = "supplier5-unique-name",
                    name = "Supplier AS5",
                )
            )
            userRepository.createUser(
                User(
                    email = adminEmail, token = password, name = "Admin tester", roles = listOf(Roles.ROLE_ADMIN)
                )
            )
            userRepository.createUser(
                User(
                    id = userId, email = userEmail, token = password, name = "User test", roles = listOf(Roles.ROLE_SUPPLIER),
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
            userAdminApiClient.createUser(jwtUser, UserRegistrationDTO (
                email = "anotheruser2@test.test", password = "test123", roles = listOf(Roles.ROLE_SUPPLIER),
                name = "another user2", attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier!!.id.toString()))
            ))
        }.isFailure shouldBe true

        // can not create a supplier user that does not belong to a supplier.
        runCatching {
            userAdminApiClient.createUser(jwtAdmin, UserRegistrationDTO (
                email = "anotheruser2@test.test", password = "test123", roles = listOf(Roles.ROLE_SUPPLIER),
                name = "another user2", attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, UUID.randomUUID().toString()))
            ))
        }.isFailure shouldBe true

        val userById = userAdminApiClient.getUser(jwtAdmin, userId).body()
        userById.shouldNotBeNull()
        userById.email shouldBe userEmail

        val userByEmail = userAdminApiClient.getUserByEmail(jwtAdmin, userEmail)
        userByEmail.body().name shouldBe "User test"

        val users = userAdminApiClient.getUsers(jwtAdmin, email = userEmail, size = 10, page = 0, sort = "updated,asc")

        users.shouldNotBeNull()
        users.totalSize shouldBe 1
        users.numberOfElements shouldBe  1
        users.content[0].email shouldBe userEmail

        // Should be two users from same supplierId
        val supplierUsers = userAdminApiClient.getUsersBySupplierId(jwtAdmin, supplierId = supplierId)
        supplierUsers.size shouldBe 2
        val oneUser = supplierUsers[0]

        val updated = userAdminApiClient.updateUser(jwtAdmin, oneUser.id, oneUser.copy(name="New Name"))
        updated.body().name shouldBe "New Name"

    }

}
