package no.nav.hm.grunndata.register.techlabel

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

@Singleton
class TechLabelMaintenance(
    val techLabelRepository: TechLabelRegistrationRepository,
    private val techLabelRegistrationService: TechLabelRegistrationService,
    val objectMapper: ObjectMapper,
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelMaintenance::class.java)

    }

    val techlabelUnitMap = objectMapper.readValue(TechLabelMaintenance::class.java.getResourceAsStream("/techlabel_unit_map.json"), Map::class.java).filter {
        it.key != it.value
    } as Map<String, String>

    suspend fun fixUnitsInTechLabel() {
        var countProductChanged = 0
        val techlabelsChanged = techLabelRepository.findAll().map { t ->
          techlabelUnitMap[t.unit]?.let { v ->
              val cUnit = if ("grader" == t.unit && t.label.indexOf("temp")>-1) "°C" else v
              LOG.info("Found wrong unit ${t.unit} for techlabel with label ${t.label} and iso ${t.isoCode}, updating to $cUnit")
              val newTechLabel = techLabelRepository.update(t.copy(unit = cUnit))
              countProductChanged += techLabelRegistrationService.changeProductsTechDataWithTechLabel(t.label, t.unit, t.isoCode, newTechLabel )
              newTechLabel
          }
        }.toList().filterNotNull()
        LOG.info("Total Changed unit ${techlabelsChanged.size} and products changed $countProductChanged")
    }
}