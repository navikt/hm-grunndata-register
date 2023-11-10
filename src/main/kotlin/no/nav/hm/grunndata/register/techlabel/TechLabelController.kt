package no.nav.hm.grunndata.register.techlabel

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.openapi.visitor.security.SecurityRule
import io.micronaut.security.annotation.Secured

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/api/v1/techlabels")
class TechLabelController(private val techLabelService: TechLabelService) {

    @Get("/")
    fun fetchTechLabels() = techLabelService.fetchAllLabels()

    @Get("/{isocode}")
    fun fetchTechLabelsByIsocode(isocode: String) = techLabelService.fetchLabelsByIsoCode(isocode)

    @Get("/name/{name}")
    fun fetchTechLabelsByName(name: String) = techLabelService.fetchLabelsByName(name)


}