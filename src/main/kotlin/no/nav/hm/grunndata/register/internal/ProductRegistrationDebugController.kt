package no.nav.hm.grunndata.register.internal

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import java.util.UUID
import no.nav.hm.grunndata.register.product.ProductRegistrationService

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/debug/products")
@Hidden
class ProductRegistrationDebugController(private val productRegistrationService: ProductRegistrationService) {

    @Get("/{id}")
    suspend fun getById(id: UUID) = productRegistrationService.findById(id)

    @Get("/hmsnr/{hmsArtNr}")
    suspend fun getByHmsNr(hmsArtNr: String) = productRegistrationService.findByHmsArtNr(hmsArtNr)

}