package no.nav.hm.grunndata.register.user

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierData
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class UserRepositoryTest(private val userRepository: UserRepository, private val supplierRegistrationService: SupplierRegistrationService) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun testUserCrud() {
        runBlocking {
            val testSupplierRegistration = supplierRegistrationService.save(
                SupplierRegistrationDTO(
                    id = UUID.randomUUID(),
                    supplierData = SupplierData (
                        email = "supplier1@test.test",
                        address = "address 1",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678"
                    ),
                    identifier = "supplier1-unique-name",
                    name = "Supplier AS1"
                )
            )
            userRepository.createUser(
                User(
                    name = "First Family",
                    email = "user@name.com",
                    token = "token123",
                    roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplierRegistration.id.toString()))
                )
            )
            val db = userRepository.findByEmailIgnoreCase("user@name.com")
            val login = userRepository.loginUser("user@name.com", "token123")
            db.shouldNotBeNull()
            login.shouldNotBeNull()
            db.name shouldBe login.name
            db.id shouldBe login.id
            db.roles shouldContain Roles.ROLE_SUPPLIER
        }
    }
}
