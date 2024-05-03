package no.nav.hm.grunndata.register.techlabel

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.openapi.visitor.security.SecurityRule
import io.micronaut.security.annotation.Secured
import io.swagger.v3.oas.annotations.tags.Tag

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/api/v1/techlabels")
@Tag(name="Vendor Techlabel")
class TechLabelController(private val techLabelService: TechLabelService) {

    @Get("/")
    fun fetchTechLabels() = techLabelService.fetchAllLabels()

    @Get("/{isocode}")
    fun fetchTechLabelsByIsocode(isocode: String) = techLabelService.fetchLabelsByIsoCode(isocode)

    @Get("/name/{name}")
    fun fetchTechLabelsByName(name: String) = techLabelService.fetchLabelsByName(name)


}