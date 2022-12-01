package no.nav.hm.grunndata.register.supplier

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.common.runBlocking
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.register.api.supplier.User
import no.nav.hm.grunndata.register.api.supplier.UserRepository
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class UserRepositoryTest(private val userRepository: UserRepository,
                         private val objectMapper: ObjectMapper) {

    @Test
    fun testUserCrud() {
        runBlocking {
            userRepository.createUser(
                User(
                    name = "User Name",
                    email = "user@name.com",
                    token = "token123",
                    supplierUuid = UUID.randomUUID()
                )
            )
            val db = userRepository.findByEmail("user@name.com")
            println(objectMapper.writeValueAsString(db))
            val login = userRepository.loginUser("user@name.com", "token123")
            println(objectMapper.writeValueAsString(login))
        }


    }
}