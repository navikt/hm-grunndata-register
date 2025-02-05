package no.nav.hm.grunndata.register.accessory

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client
import java.util.UUID
import no.nav.hm.grunndata.register.CONTEXT_PATH
import no.nav.hm.grunndata.register.product.ProductRegistrationDTOV2

@Client("$CONTEXT_PATH${AccessoryCompatibleWithController.API_V1_ACCESSORY}")
interface AccessoryCompatibleWithApiClient {

    @Get("/variants/{hmsNr}")
    suspend fun findCompatibleWithProductsVariants(@CookieValue("JWT") jwt: String, hmsNr: String): List<CompatibleProductResult>

    @Put("/{id}/compatibleWith")
    suspend fun connectProductAndVariants(@CookieValue("JWT") jwt: String, @Body compatibleWithDTO: CompatibleWithDTO, id: UUID): ProductRegistrationDTOV2

}