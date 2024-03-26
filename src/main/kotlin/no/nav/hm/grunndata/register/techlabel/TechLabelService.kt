package no.nav.hm.grunndata.register.techlabel

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.HMDB
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import org.slf4j.LoggerFactory

@Singleton
class TechLabelService(private val gdbApiClient: GdbApiClient, private val techLabelRegistrationRepository: TechLabelRegistrationRepository): LabelService {

    private var techLabelsByIso: Map<String, List<TechLabelDTO>>


    private var techLabelsByName: Map<String, List<TechLabelDTO>>

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelService::class.java)
    }

    init {
        runBlocking {
            var techLabels = techLabelRegistrationRepository.findAll().map { it.toTechLabelDTO() }.toList()
            if (techLabels.isEmpty()) {
                techLabels = gdbApiClient.fetchAllTechLabels()
                techLabels.forEach {
                    techLabelRegistrationRepository.save(
                        TechLabelRegistration(
                            isoCode = it.isocode,
                            label = it.label,
                            definition = it.definition,
                            createdByUser = HMDB,
                            updatedByUser = HMDB,
                            createdBy = HMDB,
                            updatedBy = HMDB,
                            guide = it.guide,
                            sort = it.sort,
                            identifier = it.identifier,
                            type = it.type,
                            unit = it.unit
                        )
                    )
                }
            }
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