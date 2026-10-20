package no.nav.hm.grunndata.register.techlabel

import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.iso.IsoVersion
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

    suspend fun delete(techLabelRegistration: TechLabelRegistration, forcedDelete: Boolean, isoVersion: IsoVersion = IsoVersion.v16) {
        if (!forcedDelete) {
            val products = listOf(
                IsoVersion.v16 to techLabelRegistration.isoCode,
                IsoVersion.v22 to techLabelRegistration.isoCode22,
            ).flatMap { (version, code) ->
                if (code.isNullOrBlank()) emptyList() else
                    productRegistrationService.findByStartsWithIsoCategoryAndTechLabelKeyUnit(
                        code, techLabelRegistration.label, techLabelRegistration.unit, version,
                    )
            }.distinctBy { it.id }
            products
                .let { products ->
                    if (products.isNotEmpty()) {
                        throw IllegalStateException("${techLabelRegistration.label} (${techLabelRegistration.unit}) " +
                                "for ${techLabelRegistration.isoCode} er brukt av ${products.size} produkter!")
                    }
                }
        }
        techLabelRegistrationRepository.delete(techLabelRegistration)
    }

    suspend fun changeProductsTechDataWithTechLabel(oldTechLabel: TechLabelRegistration, newTechLabel: TechLabelRegistration) {
        val oldKey = oldTechLabel.label
        val oldUnit = oldTechLabel.unit
        val categoryCodes = listOf(
            IsoVersion.v16 to oldTechLabel.isoCode,
            IsoVersion.v22 to oldTechLabel.isoCode22,
        ).mapNotNull { (version, code) ->
            code?.takeIf { it.isNotBlank() }?.let { version to it }
        }
        categoryCodes.forEach { (_, code) ->
            require(code.length >= 6) {
                "IsoCode must be at least 6 characters to avoid too broad search, was $code"
            }
        }
        val products = categoryCodes.flatMap { (version, code) ->
            productRegistrationService.findByStartsWithIsoCategoryAndTechLabelKeyUnit(
                code, key = oldKey, unit = oldUnit, isoVersion = version,
            )
        }.distinctBy { it.id }
        LOG.debug("Found ${products.size} products with techLabel key=$oldKey, unit=$oldUnit to update to new techLabel ${newTechLabel.label} (${newTechLabel.unit})")
        products.forEach { product ->
            val techData = product.productData.techData.toMutableList()
            val techLabel = techData.find { it.key == oldKey && it.unit == oldUnit }
            techLabel?.let {
                techData.remove(it)
                LOG.debug("Changed from ${it.key} to ${newTechLabel.label} and ${it.unit} to ${newTechLabel.unit} for product ${product.hmsArtNr}")
                techData.add(it.copy(key = newTechLabel.label, unit = newTechLabel.unit ?: ""))
            }
            val updatedProdData = product.productData.copy(techData = techData)
            productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
        }

    }

    suspend fun findByLabelAndIsoCode(label: String, isoCode: String) =
        techLabelRegistrationRepository.findByLabelAndIsoCode(label, isoCode)

    suspend fun updateSectionForLabel(label: String, section: String?): List<TechLabelRegistration> {
        val techLabels = techLabelRegistrationRepository.findByLabel(label)
        return techLabels.map { techLabelRegistrationRepository.update(it.copy(section = section)) }
    }

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(TechLabelRegistrationService::class.java)
    }

}