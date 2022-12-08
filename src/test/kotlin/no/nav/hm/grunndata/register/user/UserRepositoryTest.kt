package no.nav.hm.grunndata.register.user

import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.register.security.Roles
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class UserRepositoryTest(private val userRepository: UserRepository) {

    @Test
    fun testUserCrud() {
        runBlocking {
            userRepository.createUser(
                User(
                    name = "First Family",
                    email = "user@name.com",
                    token = "token123",
                    supplierUuid = UUID.randomUUID(),
                    roles = listOf(Roles.ROLE_SUPPLIER),
                    attributes = emptyMap()
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