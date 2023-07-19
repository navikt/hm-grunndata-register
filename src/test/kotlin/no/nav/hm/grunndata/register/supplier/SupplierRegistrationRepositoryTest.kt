package no.nav.hm.grunndata.register.supplier

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import no.nav.hm.rapids_rivers.micronaut.RapidPushService
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class SupplierRegistrationRepositoryTest(private val supplierRegistrationService: SupplierRegistrationService) {

    @MockBean(RapidPushService::class)
    fun rapidPushService(): RapidPushService = mockk(relaxed = true)

    @Test
    fun crudSupplierTest() {
        val name = "Leverandør AS ${UUID.randomUUID()}"
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
            val saved = supplierRegistrationService.save(supplierRegistration)
            saved.shouldNotBeNull()
            saved.id shouldBe supplierRegistration.id
            val inDb = supplierRegistrationService.findById(saved.id)
            inDb.shouldNotBeNull()
            inDb.name shouldBe name
            val updated = supplierRegistrationService.update(inDb.copy(name = "Leverandør AS-2"))
            updated.shouldNotBeNull()
            updated.name shouldBe "Leverandør AS-2"
            updated.supplierData.email shouldBe "email@email.com"
            val deactivated = supplierRegistrationService.update(updated.copy(status = SupplierStatus.INACTIVE))
            deactivated.shouldNotBeNull()
            deactivated.status shouldBe SupplierStatus.INACTIVE
        }
    }

}
