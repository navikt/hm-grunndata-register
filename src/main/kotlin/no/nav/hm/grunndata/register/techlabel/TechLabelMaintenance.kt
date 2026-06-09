package no.nav.hm.grunndata.register.techlabel

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.data.runtime.criteria.get
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
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


    suspend fun normalizeTechLabelsAndValues() {
        LOG.info("Reading unit mapping")
        val techlabelUnitMap = objectMapper.readValue(
            TechLabelMaintenance::class.java.getResourceAsStream("/techlabel_unit_map.json"),
            Map::class.java
        ).filter {
            it.key != it.value
        } as Map<String, String>

        LOG.info("Reading label names mapping")
        val techLabelNormalized = objectMapper.readValue(
            TechLabelMaintenance::class.java.getResourceAsStream("/techlabels_normalized.json"),
            object : TypeReference<List<TechLabelMapping>>() {}).filter {
            it.original != it.normalized
        }

        LOG.info("Reading option values mapping")
        val techLabelOptionValues = objectMapper.readValue(
            TechLabelMaintenance::class.java.getResourceAsStream("/techlabels_options_value_normalized.json"),
            object : TypeReference<List<TechLabelOptionMapping>>() {}
        ).groupBy { it.label to it.iso_code }.mapValues { entry ->
            entry.value.associate { it.value to it.normalized }
        }
        var cUpdate = 0

        // normalize units
        techLabelRepository.findAll().collect { techInDb ->
            techlabelUnitMap[techInDb.unit]?.let { v ->
                val cUnit = if ("grader" == techInDb.unit && techInDb.label.indexOf("temp") > -1) "°C" else v
                LOG.info("Found wrong unit ${techInDb.unit} for techlabel with label ${techInDb.label} and iso ${techInDb.isoCode}, updating to $cUnit")
                cUpdate += 1
                techLabelRepository.update(techInDb.copy(unit = cUnit))
            }
        }
        LOG.info("Normalized units inDb count: $cUpdate")
        cUpdate = 0
        //normalize label
        techLabelRepository.findAll().collect { inDb ->
            techLabelNormalized.find { it.original == inDb.label }?.let { mapped ->
                LOG.info("Found unnormalized label ${inDb.label} for techlabel with iso ${inDb.isoCode}, updating to ${mapped.normalized}")
                cUpdate += 1
                techLabelRepository.update(inDb.copy(label = mapped.normalized))
            }
        }

        LOG.info("Normalized labels inDb count: $cUpdate")
        cUpdate = 0
        // normalize option values in db
        techLabelRepository.findAll().filter { it.options.isNotEmpty() }.collect { inDb ->
            techLabelOptionValues[inDb.label to inDb.isoCode]?.let { options ->
                if (options.values.size == inDb.options.size) {
                    val options = options.values.toSet()
                    val inDbOptions = inDb.options
                    if (options != inDbOptions) {
                        LOG.info("Updating ${inDb.label} ${inDb.isoCode} to ${options}")
                        cUpdate += 1
                        techLabelRepository.update(inDb.copy(options = options))
                    }
                }
            }
        }
        LOG.info("Normalize options values inDb count:  $cUpdate")
        cUpdate = 0

        // change products
        val products = productRegistrationService.findAll(spec = where {
            root[ProductRegistration::mainProduct] eq true
        })
        // transform option values, so that it easier to match from products tech data
        val optionValuesLabelMap = techLabelOptionValues.map { entry ->
            val label = entry.key.first
            label to entry.value
        }.toMap()

        var countChanged = 0
        var countProducts = 0
        products.collect { product ->
            countProducts++
            val techData = product.productData.techData.toMutableList()
            var changed = false

            techData.forEachIndexed { index, techLabel ->
                if (techLabel.value.isNotEmpty()) {
                    // normalize units
                    techlabelUnitMap[techLabel.unit]?.let { v ->
                        val cUnit = if ("grader" == techLabel.unit && techLabel.key.indexOf("temp") > -1) "°C" else v
                        LOG.info("Found wrong unit ${techLabel.unit} with key ${techLabel.key} , updating to $cUnit")
                        techData[index] = techLabel.copy(unit = cUnit)
                        changed = true
                    }
                    // normalize labels
                    techLabelNormalized.find { it.original == techLabel.key }?.let { mapped ->
                        LOG.info("Found unnormalized label ${techLabel.key}, updating to ${mapped.normalized}")
                        techData[index] = techLabel.copy(key = mapped.normalized)
                        changed = true
                    }
                    // normalize options values
                    optionValuesLabelMap[techLabel.key]?.let { v ->
                        val techValue = techLabel.value.trim()
                        val value = v[techValue]
                        if (value!=null && value != techValue ) {
                            LOG.info("Found unnormalized option value ${techLabel.value} for label ${techLabel.key}, updating to ${value}")
                            techData[index] = techLabel.copy(value = value)
                            changed = true
                        }
                    }
                }
            }
            if (changed) {
                countChanged++
                LOG.info("Changed product ${product.id} with hmsArtNr ${product.hmsArtNr}")
                val updatedProdData = product.productData.copy(techData = techData)
                productRegistrationService.saveAndCreateEventIfNotDraftAndApproved(
                    product.copy(productData = updatedProdData),
                    true
                )
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

data class TechLabelOptionMapping(
    val value: String,
    val normalized: String,
    val label: String,
    val iso_code: String
)