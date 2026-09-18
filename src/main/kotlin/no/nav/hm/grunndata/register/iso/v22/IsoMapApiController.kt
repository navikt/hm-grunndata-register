package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.openapi.visitor.security.SecurityRule
import io.micronaut.security.annotation.Secured
import io.swagger.v3.oas.annotations.tags.Tag

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/api/v22/isomap")
@Tag(name="Vendor IsoCategory 2016 to IsoCategory 2022 mapping")
class IsoMapApiController(private val isoMapService: IsoMapService) {

    @Get("/")
    fun getAllMappings(): List<IsoMapDTO> = isoMapService.retrieveAll()

}