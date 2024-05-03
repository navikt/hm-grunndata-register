package no.nav.hm.grunndata.register.iso

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.openapi.visitor.security.SecurityRule
import io.micronaut.security.annotation.Secured
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.hm.grunndata.rapid.dto.IsoCategoryDTO

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/api/v1/isocategories")
@Tag(name="Vendor IsoCategory")
class IsoCategoryController(private val isoCategoryService: IsoCategoryService) {

    @Get("/")
    fun getAllCategories(): List<IsoCategoryDTO> =isoCategoryService.retrieveAllCategories()

    @Get("/{isocode}")
    fun getCategoryByIsocode(isocode: String): IsoCategoryDTO? = isoCategoryService.lookUpCode(isocode)
}