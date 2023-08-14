package no.nav.hm.grunndata.register.techlabel

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import org.slf4j.LoggerFactory

@Singleton
class TechLabelService(private val gdbApiClient: GdbApiClient) {

    private val techLabels: Map<String, List<TechLabelDTO>> =
        gdbApiClient.fetchAllTechLabels().groupBy { it.isocode }

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelService::class.java)
    }

    init {
        LOG.info("Init techlabels size ${techLabels.size}")
    }

    fun fetchLabelsByIsoCode(isocode: String): List<TechLabelDTO>? = techLabels[isocode]

    fun fetchAllLabels(): Map<String, List<TechLabelDTO>> = techLabels

}