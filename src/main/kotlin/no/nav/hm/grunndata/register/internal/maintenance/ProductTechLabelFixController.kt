package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/techlabel")
@Hidden
class ProductTechLabelFixController(private val productRegistrationService: ProductRegistrationService) {

    @Put("/")
    suspend fun fixProductTechlabel() {
        val Ja = productRegistrationService.findByTechLabelValues(value = "JA")
        LOG.info("Found ${Ja.size} products with tech label value 'JA'")
        Ja.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.filter { it.value == "JA" }
            techLabel.forEach {
                techData.remove(it)
                techData.add(it.copy(value = "Ja"))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }
        val Nei = productRegistrationService.findByTechLabelValues(value = "NEI")
        LOG.info("Found ${Nei.size} products with tech label value 'NEI'")
        Nei.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.filter { it.value == "NEI" }
            techLabel.forEach {
                techData.remove(it)
                techData.add(it.copy(value = "Nei"))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductTechLabelFixController::class.java)
    }
}