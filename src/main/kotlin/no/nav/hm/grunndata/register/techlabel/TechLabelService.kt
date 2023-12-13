package no.nav.hm.grunndata.register.techlabel

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import org.slf4j.LoggerFactory

@Singleton
class TechLabelService(private val gdbApiClient: GdbApiClient) {

    private var techLabelsByIso: Map<String, List<TechLabelDTO>>


    private var techLabelsByName: Map<String, List<TechLabelDTO>>

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelService::class.java)
    }

    init {
        runBlocking {
            val techLabels = gdbApiClient.fetchAllTechLabels()
            techLabelsByIso = techLabels.groupBy { it.isocode }
            techLabelsByName = techLabels.groupBy { it.label }
            LOG.info("Init techlabels size ${techLabelsByIso.size}")
        }
    }

    fun fetchLabelsByIsoCode(isocode: String): List<TechLabelDTO>? = techLabelsByIso[isocode]

    fun fetchLabelsByName(name: String): List<TechLabelDTO>? = techLabelsByName[name]

    fun fetchAllLabels(): Map<String, List<TechLabelDTO>> = techLabelsByIso

}