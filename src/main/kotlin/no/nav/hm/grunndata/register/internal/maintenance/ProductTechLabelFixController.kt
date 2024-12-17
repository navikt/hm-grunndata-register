package no.nav.hm.grunndata.register.internal.maintenance

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Hidden
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import org.slf4j.LoggerFactory

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/internal/fix/techlabel")
@Hidden
class ProductTechLabelFixController(private val productRegistrationService: ProductRegistrationService) {

    @Get("/")
    suspend fun fixProductTechlabel() {
        val products = productRegistrationService.findByTechLabelValues("Brukerhøyde maks", "kg")
        LOG.info("Found ${products.size} products with tech label 'Brukerhøyde maks' and unit 'kg'")
        products.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.find { it.key == "Brukerhøyde maks" && it.unit == "kg" }
            techLabel?.let {
                techData.remove(it)
                techData.add(it.copy(key = "Brukerhøyde maks", unit = "cm"))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }

        val Ja = productRegistrationService.findByTechLabelValues( value = "Ja")
        LOG.info("Found ${Ja.size} products with tech label value 'Ja'")
        Ja.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.filter { it.value == "Ja" }
            techLabel.forEach {
                techData.remove(it)
                techData.add(it.copy(value = "JA"))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }
        val Nei = productRegistrationService.findByTechLabelValues( value = "Nei")
        LOG.info("Found ${Nei.size} products with tech label value 'Nei'")
        Nei.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.filter { it.value == "Nei" }
            techLabel.forEach {
                techData.remove(it)
                techData.add(it.copy(value = "NEI"))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }

        val ja = productRegistrationService.findByTechLabelValues( value = "ja")
        LOG.info("Found ${ja.size} products with tech label value 'ja'")
        ja.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.filter { it.value == "ja" }
            techLabel.forEach {
                techData.remove(it)
                techData.add(it.copy(value = "JA"))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }
        val nei = productRegistrationService.findByTechLabelValues( value = "nei")
        LOG.info("Found ${nei.size} products with tech label value 'nei'")
        nei.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.filter { it.value == "nei" }
            techLabel.forEach {
                techData.remove(it)
                techData.add(it.copy(value = "NEI"))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductTechLabelFixController::class.java)
    }
}