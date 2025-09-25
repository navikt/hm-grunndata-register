package no.nav.hm.grunndata.register.techlabel

import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.product.ProductRegistrationService

import java.util.*


@Singleton
class TechLabelRegistrationService(private val techLabelRegistrationRepository: TechLabelRegistrationRepository,
                                   private val productRegistrationService: ProductRegistrationService) {

    suspend fun findById(id: UUID) = techLabelRegistrationRepository.findById(id)
    suspend fun findAll(spec:  PredicateSpecification<TechLabelRegistration>? = null, pageable: Pageable) =
        techLabelRegistrationRepository.findAll(spec, pageable)

    suspend fun save(techlabel: TechLabelRegistration) = techLabelRegistrationRepository.save(techlabel)

    suspend fun update(techlabel: TechLabelRegistration) = techLabelRegistrationRepository.update(techlabel)

    suspend fun changeProductsTechDataWithTechLabel(oldKey: String, oldUnit: String?, isoCode: String, newTechLabel: TechLabelRegistration) {
        val products = productRegistrationService.findByIsoCategoryAndTechLabelKeyUnit(isoCode, key = oldKey, unit = oldUnit)
        LOG.info("Found ${products.size} products with techLabel key=$oldKey, unit=$oldUnit to update to new techLabel ${newTechLabel.label} (${newTechLabel.unit})")
        products.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.find { it.key == oldKey && it.unit == oldUnit }
            techLabel?.let {
                techData.remove(it)
                techData.add(it.copy(key = newTechLabel.label, unit = newTechLabel.unit ?: ""))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }
    }

    suspend fun findByLabelAndIsoCode(label: String, isoCode: String) =
        techLabelRegistrationRepository.findByLabelAndIsoCode(label, isoCode)

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(TechLabelRegistrationService::class.java)
    }

}