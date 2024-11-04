package no.nav.hm.grunndata.register.supplier

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.data.model.Pageable
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class SupplierRegistrationServiceTest(private val supplierRegistrationService: SupplierRegistrationService) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun testSupplierRegistrationService() {
        val name = "leverandør as ${UUID.randomUUID()}"
        val supplierRegistration = SupplierRegistrationDTO(
            id = UUID.randomUUID(),
            name = name,
            supplierData = SupplierData(
                address = "veien 1",
                homepage = "www.hjemmesiden.no",
                phone = "+47 12345678",
                email = "email@email.com"),
            identifier = name)

        runBlocking {
            val dto = supplierRegistrationService.save(supplierRegistration)
            dto.shouldNotBeNull()
            dto.name shouldBe name
        }
    }
}