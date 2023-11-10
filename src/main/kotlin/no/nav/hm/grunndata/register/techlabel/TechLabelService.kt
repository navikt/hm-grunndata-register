package no.nav.hm.grunndata.register.techlabel

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import org.slf4j.LoggerFactory

@Singleton
class TechLabelService(private val gdbApiClient: GdbApiClient) {

    private val techLabelsByIso: Map<String, List<TechLabelDTO>> =
        gdbApiClient.fetchAllTechLabels().groupBy { it.isocode }

    private val techLabelsByName: Map<String, List<TechLabelDTO>> =
        gdbApiClient.fetchAllTechLabels().groupBy { it.label }



    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelService::class.java)
    }

    init {
        LOG.info("Init techlabels size ${techLabelsByIso.size}")
    }

    fun fetchLabelsByIsoCode(isocode: String): List<TechLabelDTO>? = techLabelsByIso[isocode]

    fun fetchLabelsByName(name: String): List<TechLabelDTO>? = techLabelsByName[name]

    fun fetchAllLabels(): Map<String, List<TechLabelDTO>> = techLabelsByIso

}