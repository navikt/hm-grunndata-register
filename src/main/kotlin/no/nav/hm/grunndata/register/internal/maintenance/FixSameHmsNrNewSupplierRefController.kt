package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden

@Controller("/internal/fix/same-hmsnr-new-ref")
@Secured(SecurityRule.IS_ANONYMOUS)
@Hidden
class FixSameHmsNrNewSupplierRefController(private val fixSameHmsNrNewSupplierRef: FixSameHmsNrNewSupplierRef) {

    @Put("/{hmsNr}/{supplierRef}")
    suspend fun fixSameHmsNrNewSupplierRef(hmsNr: String, supplierRef: String) {
        fixSameHmsNrNewSupplierRef.fixProductThatChangedSupplierRef(newSupplierRef = supplierRef, hmsNr = hmsNr)
    }
}