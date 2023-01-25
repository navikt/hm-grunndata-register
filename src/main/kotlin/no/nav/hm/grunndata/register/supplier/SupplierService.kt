package no.nav.hm.grunndata.register.supplier

class SupplierService(private val supplierRepository: SupplierRepository) {

    suspend fun save(supplier: Supplier): Supplier = supplierRepository.findById(supplier.id)?.let {
        supplierRepository.update(supplier.copy(created = it.created)) } ?: supplierRepository.save(supplier)


}