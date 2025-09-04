package no.nav.hm.grunndata.register.internal.maintenance

import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import no.nav.hm.grunndata.register.catalog.CatalogImportRepository
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.productagreement.ProductAgreementRegistrationRepository

@Singleton
open class FixSameHmsNrNewSupplierRef(private val productRegistrationService: ProductRegistrationService,
                                 private val productAgreementRegistrationRepository: ProductAgreementRegistrationRepository,
                                 private val catalogImportRepository: CatalogImportRepository
) {
    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(FixSameHmsNrNewSupplierRef::class.java)
    }

    @Transactional
    open suspend fun fixProductThatChangedSupplierRef(newSupplierRef: String, hmsNr: String) {
        val pags = productAgreementRegistrationRepository.findByHmsArtNr(hmsNr)
        val productId = pags.firstOrNull()?.productId
        val catalogImports = catalogImportRepository.findByHmsArtNr(hmsNr)
        catalogImports.forEach {
            LOG.info("Changing supplierRef for catalogImport ${it.hmsArtNr} from ${it.supplierRef} to $newSupplierRef")
            catalogImportRepository.update(it.copy(supplierRef = newSupplierRef))
        }
        pags.forEach {
            LOG.info("Changing supplierRef for pag productId ${it.productId} from ${it.supplierRef} to $newSupplierRef")
            productAgreementRegistrationRepository.update(it.copy(supplierRef = newSupplierRef))

        }
        if (productId != null) {
            productRegistrationService.findById(productId)?.let {
                LOG.info("Changing supplierRef for product ${it.id} from ${it.supplierRef} to $newSupplierRef")
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(it.copy(supplierRef = newSupplierRef), isUpdate = true)
            }
        }
    }

}