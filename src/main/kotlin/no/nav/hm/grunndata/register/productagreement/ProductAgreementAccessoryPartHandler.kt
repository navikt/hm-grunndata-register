package no.nav.hm.grunndata.register.productagreement

import jakarta.inject.Singleton

@Singleton
class ProductAgreementAccessoryPartHandler {

    fun handleAccessoryAndSparePartProductAgreement(productAgreements: List<ProductAgreementRegistrationDTO>) {
        // group products, accessory and spare parts based on the title that has intersection size > 2
        val mainProducts = productAgreements.filter { !it.accessory && !it.sparePart }
        val accessoryOrSpareParts = productAgreements.filter { it.accessory || it.sparePart }


    }
}