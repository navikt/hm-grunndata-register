package no.nav.hm.grunndata.register.techlabel

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.data.runtime.criteria.get
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.product.ProductRegistration
import no.nav.hm.grunndata.register.product.ProductRegistrationService
import no.nav.hm.grunndata.register.runtime.where
import org.slf4j.LoggerFactory

@Singleton
class TechLabelMaintenance(
    val techLabelRepository: TechLabelRegistrationRepository,
    val productRegistrationService: ProductRegistrationService,
    val objectMapper: ObjectMapper,
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelMaintenance::class.java)

    }

    val techlabelUnitMap = objectMapper.readValue(
        TechLabelMaintenance::class.java.getResourceAsStream("/techlabel_unit_map.json"),
        Map::class.java
    ).filter {
        it.key != it.value
    } as Map<String, String>

    val techLabelNormalized = objectMapper.readValue(
        TechLabelMaintenance::class.java.getResourceAsStream("/techlabels_normalized.json"),
        object : TypeReference<List<TechLabelMapping>>() {}).filter {
        it.original != it.normalized
    }

    suspend fun fixUnitsInTechLabel() {
        val techlabelsChanged = techLabelRepository.findAll().map { t ->
            techlabelUnitMap[t.unit]?.let { v ->
                val cUnit = if ("grader" == t.unit && t.label.indexOf("temp") > -1) "°C" else v
                LOG.info("Found wrong unit ${t.unit} for techlabel with label ${t.label} and iso ${t.isoCode}, updating to $cUnit")
                val newTechLabel = techLabelRepository.update(t.copy(unit = cUnit))
                newTechLabel
            }
        }.toList().filterNotNull()
        LOG.info("Total Changed unit ${techlabelsChanged.size}")
    }

    suspend fun normalizeLabels() {
        val techlabelsChanged = techLabelRepository.findAll().map { inDb ->
            techLabelNormalized.find { it.original == inDb.label }?.let { mapped ->
                LOG.info("Found unnormalized label ${inDb.label} for techlabel with iso ${inDb.isoCode}, updating to ${mapped.normalized}")
                val newTechLabel = techLabelRepository.update(inDb.copy(label = mapped.normalized))
                newTechLabel
            }
        }.toList().filterNotNull()
        LOG.info("Total Changed label ${techlabelsChanged.size}")
    }

    suspend fun fixProductTechLabels() {
        val products = productRegistrationService.findAll(spec = where {
            root[ProductRegistration::mainProduct] eq true
        })
        var countChanged = 0
        var countProducts = 0
        products.collect { product ->
            countProducts++
            val techData = product.productData.techData.toMutableList()
            var changed = false

            techData.forEachIndexed { index, techLabel ->
                techlabelUnitMap[techLabel.unit]?.let { v ->
                    val cUnit = if ("grader" == techLabel.unit && techLabel.key.indexOf("temp") > -1) "°C" else v
                    LOG.info("Found wrong unit ${techLabel.unit} for techlabel with key ${techLabel.key} in product ${product.id}, updating to $cUnit")
                    techData[index] = techLabel.copy(unit = cUnit)
                    changed = true
                }
                techLabelNormalized.find { it.original == techLabel.key }?.let { mapped ->
                    LOG.info("Found unnormalized label ${techLabel.key} for techlabel in product ${product.id}, updating to ${mapped.normalized}")
                    techData[index] = techLabel.copy(key = mapped.normalized)
                    changed = true
                }
            }
            if (changed) {
                countChanged++
                LOG.info("Changed product ${product.id} with hmsArtNr ${product.hmsArtNr}")
                val updatedProdData = product.productData.copy(techData = techData)
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(product.copy(productData = updatedProdData), true)
            }
        }
        LOG.info("Total Changed products $countChanged out of $countProducts")
    }
}

data class TechLabelMapping(
    val original: String,
    val normalized: String,
    val category: String
)