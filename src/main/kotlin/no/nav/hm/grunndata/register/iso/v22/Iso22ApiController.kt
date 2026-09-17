package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.openapi.visitor.security.SecurityRule
import io.micronaut.security.annotation.Secured
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.IsoCategory22DTO

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/api/v22/isocategories")
@Tag(name="Vendor IsoCategory22")
class Iso22ApiController(private val iso22Service: Iso22Service) {

    @Get("/")
    fun getAllCategories(): List<IsoCategory22DTO> = iso22Service.retrieveAll()

    @Get("/{isocode}")
    fun getCategoryByIsocode(isocode: String): IsoCategory22DTO? = iso22Service.lookUpCode(isocode)

}