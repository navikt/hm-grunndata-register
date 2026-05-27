package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.techlabel.TechLabelMaintenance

@Hidden
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/techlabel/fix")
class TechLabelFixController(val techLabelMaintenance: TechLabelMaintenance) {

    @Put("/units")
    suspend fun fixUnits() {
        techLabelMaintenance.fixUnitsInTechLabel()
    }

    @Put("/labels")
    suspend fun fixLabels() {
        techLabelMaintenance.normalizeLabels()
    }

    @Put("/products")
    suspend fun fixProducts() {
        techLabelMaintenance.fixProductTechLabels()
    }

}