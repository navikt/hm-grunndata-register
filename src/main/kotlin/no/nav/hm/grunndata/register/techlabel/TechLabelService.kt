package no.nav.hm.grunndata.register.techlabel

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import org.slf4j.LoggerFactory

@Singleton
class TechLabelService(private val gdbApiClient: GdbApiClient): LabelService {

    private var techLabelsByIso: Map<String, List<TechLabelDTO>>


    private var techLabelsByName: Map<String, List<TechLabelDTO>>

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelService::class.java)
    }

    init {
        runBlocking {
            val techLabels = gdbApiClient.fetchAllTechLabels()
            LOG.info("Init techlabels size ${techLabels.size}")
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