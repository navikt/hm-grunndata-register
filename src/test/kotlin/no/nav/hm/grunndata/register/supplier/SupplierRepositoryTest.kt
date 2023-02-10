package no.nav.hm.grunndata.register.supplier

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.dto.SupplierInfo
import no.nav.hm.grunndata.dto.SupplierStatus
import org.junit.jupiter.api.Test

@MicronautTest
class SupplierRepositoryTest(private val supplierRepository: SupplierRepository) {

    @Test
    fun crudSupplierTest() {
        val supplier = Supplier(
            name = "Leverandør AS",
            info = SupplierInfo(
                address = "veien 1",
                homepage = "www.hjemmesiden.no",
                phone = "+47 12345678",
                email = "email@email.com"),
            identifier = "leverandor-as")
        runBlocking {
            val saved = supplierRepository.save(supplier)
            saved.shouldNotBeNull()
            saved.id shouldBe supplier.id
            val inDb = supplierRepository.findById(saved.id)
            inDb.shouldNotBeNull()
            inDb.name shouldBe "Leverandør AS"
            val updated = supplierRepository.update(inDb.copy(name = "Leverandør AS-2"))
            updated.shouldNotBeNull()
            updated.name shouldBe "Leverandør AS-2"
            updated.info.email shouldBe "email@email.com"
            val deactivated = supplierRepository.update(updated.copy(status = SupplierStatus.INACTIVE))
            deactivated.shouldNotBeNull()
            deactivated.status shouldBe SupplierStatus.INACTIVE
        }
    }

}