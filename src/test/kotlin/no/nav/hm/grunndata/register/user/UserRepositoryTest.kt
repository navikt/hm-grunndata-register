package no.nav.hm.grunndata.register.user

import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.SupplierInfo
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.Supplier
import no.nav.hm.grunndata.register.supplier.SupplierRepository
import no.nav.hm.grunndata.register.supplier.SupplierService
import org.junit.jupiter.api.Test

@MicronautTest
class UserRepositoryTest(private val userRepository: UserRepository, private val supplierService: SupplierService) {

    @Test
    fun testUserCrud() {
        runBlocking {
            val testSupplier = supplierService.save(
                Supplier(
                    info = SupplierInfo(
                        email = "supplier1@test.test",
                        address = "address 1",
                        homepage = "https://www.hompage.no",
                        phone = "+47 12345678"
                    ),
                    identifier = "supplier1-unique-name",
                    name = "Supplier AS1",

                )
            )
            userRepository.createUser(
                User(
                    name = "First Family",
                    email = "user@name.com",
                    token = "token123",
                    roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = mapOf(Pair(UserAttribute.SUPPLIER_ID, testSupplier.id.toString()))
                )
            )
            val db = userRepository.findByEmail("user@name.com")
            val login = userRepository.loginUser("user@name.com", "token123")
            db.shouldNotBeNull()
            login.shouldNotBeNull()
            db.name shouldBe login.name
            db.id shouldBe login.id
            db.roles shouldContain Roles.ROLE_SUPPLIER
        }
    }
}
