package no.nav.hm.grunndata.register.productagreement


data class ProductAgreementImportDTO(val dryRun: Boolean,
                                     val count: Int,
                                     val productAgreements: List<ProductAgreementRegistrationDTO>)