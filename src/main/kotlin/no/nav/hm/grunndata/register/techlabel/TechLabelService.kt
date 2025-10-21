package no.nav.hm.grunndata.register.techlabel

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

@Singleton
@CacheConfig(cacheNames = ["techlabels"])
open class TechLabelService(
    private val techLabelRegistrationRepository: TechLabelRegistrationRepository
) : LabelService {

    companion object {
        private val LOG = LoggerFactory.getLogger(TechLabelService::class.java)
    }

    override fun fetchLabelsByIsoCode(isocode: String): List<TechLabelDTO> = runBlocking {
        LOG.info("Fetching labels by isocode: $isocode")
        val levels = isocode.length / 2
        val techLabels: MutableList<TechLabelDTO> = mutableListOf()
        for (i in levels downTo 0) {
            val iso = isocode.substring(0, i * 2)
            techLabels.addAll(fetchAllLabels()[iso] ?: emptyList())
        }
        techLabels.distinctBy { it.id }
    }

    override fun fetchLabelsByName(name: String): List<TechLabelDTO> = runBlocking {
        fetchAllLabelsGroupByName()[name] ?: emptyList()
    }

    @Cacheable
    override fun fetchAllLabels(): Map<String, List<TechLabelDTO>> = runBlocking {
        LOG.info("Fetching labels list")
        val techLabels = techLabelRegistrationRepository.findAll().map { it.toTechLabelDTO()}.toList()
        techLabels.groupBy {  it.isocode }
    }

    fun fetchAllLabelsGroupByName(): Map<String, List<TechLabelDTO>> = runBlocking {
        val techLabels = fetchAllLabels().values.flatten()
        techLabels.groupBy {  it.label }
    }

}