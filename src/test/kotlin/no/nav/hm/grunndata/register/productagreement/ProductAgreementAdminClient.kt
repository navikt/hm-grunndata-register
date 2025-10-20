package no.nav.hm.grunndata.register.productagreement

import io.micronaut.http.MediaType.APPLICATION_JSON
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.CookieValue
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import no.nav.hm.grunndata.register.CONTEXT_PATH


@Client("${CONTEXT_PATH}/${ProductAgreementAdminController.ADMIN_API_V1_PRODUCT_AGREEMENT}")
interface ProductAgreementAdminClient {

    @Post("/", processes = [APPLICATION_JSON])
    fun createProductAgreement(
        @CookieValue("JWT") jwt: String,
        @Body regDTO: ProductAgreementRegistrationDTO
    ): ProductAgreementRegistrationDTO

}