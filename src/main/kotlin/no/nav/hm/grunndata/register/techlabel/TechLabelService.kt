package no.nav.hm.grunndata.register.techlabel

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

@Singleton
class TechLabelService(private val techLabelRegistrationRepository: TechLabelRegistrationRepository): LabelService {

    private var techLabelsByIso: Map<String, List<TechLabelDTO>>


    private var techLabelsByName: Map<String, List<TechLabelDTO>>

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelService::class.java)
    }

    init {
        runBlocking {
            val techLabels = techLabelRegistrationRepository.findAll().map { it.toTechLabelDTO() }.toList()
            if (techLabels.size<1000) {
                LOG.error("Tech labels are not loaded properly, only ${techLabels.size} loaded")
            }
            techLabelsByIso = techLabels.groupBy { it.isocode }
            techLabelsByName = techLabels.groupBy { it.label }
        }
    }

    override fun fetchLabelsByIsoCode(isocode: String): List<TechLabelDTO> {
        val levels = isocode.length/2
        val techLabels: MutableList<TechLabelDTO> = mutableListOf()
        for (i in levels downTo 0) {
            val iso = isocode.substring(0, i*2)
            techLabels.addAll(techLabelsByIso[iso] ?: emptyList())
        }
        return techLabels.distinctBy { it.id }
    }

    override fun fetchLabelsByName(name: String): List<TechLabelDTO>? = techLabelsByName[name]

    override fun fetchAllLabels(): Map<String, List<TechLabelDTO>> = techLabelsByIso

}