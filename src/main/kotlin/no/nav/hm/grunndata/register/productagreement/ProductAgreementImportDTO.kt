package no.nav.hm.grunndata.register.productagreement

import no.nav.hm.grunndata.register.supplier.SupplierRegistrationDTO

data class ProductAgreementImportDTO(val dryRun: Boolean,
                                     val count: Int,
                                     val productAgreements: List<ProductAgreementRegistrationDTO>)