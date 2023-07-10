package no.nav.hm.grunndata.register.supplier

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.SupplierInfo
import no.nav.hm.grunndata.rapid.dto.SupplierStatus
import org.junit.jupiter.api.Test
import java.util.*

@MicronautTest
class SupplierRepositoryTest(private val supplierService: SupplierService) {

    @Test
    fun crudSupplierTest() {
        val name = "Leverandør AS ${UUID.randomUUID()}"
        val supplier = Supplier(
            name = name,
            info = SupplierInfo(
                address = "veien 1",
                homepage = "www.hjemmesiden.no",
                phone = "+47 12345678",
                email = "email@email.com"),
            identifier = name)
        runBlocking {
            val saved = supplierService.save(supplier)
            saved.shouldNotBeNull()
            saved.id shouldBe supplier.id
            val inDb = supplierService.findById(saved.id)
            inDb.shouldNotBeNull()
            inDb.name shouldBe name
            val updated = supplierService.update(inDb.copy(name = "Leverandør AS-2"))
            updated.shouldNotBeNull()
            updated.name shouldBe "Leverandør AS-2"
            updated.info.email shouldBe "email@email.com"
            val deactivated = supplierService.update(updated.copy(status = SupplierStatus.INACTIVE))
            deactivated.shouldNotBeNull()
            deactivated.status shouldBe SupplierStatus.INACTIVE
        }
    }

}
