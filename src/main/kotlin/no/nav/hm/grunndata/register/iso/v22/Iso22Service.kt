package no.nav.hm.grunndata.register.iso.v22

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import org.slf4j.LoggerFactory

@Singleton
class Iso22Service(
    private val iso22Repository: Iso22Repository,
    private val isoMapRepository: IsoMapRepository

) {

    private var iso22Categories: Map<String, Iso22>


    var isoMaps: Map<String, IsoMap> = emptyMap()

    companion object {
        private val LOG = LoggerFactory.getLogger(IsoCategoryService::class.java)
    }

    init {
        runBlocking {
            iso22Categories = iso22Repository.findAll().toList().associateBy { it.isoCode }
            isoMaps = isoMapRepository.findAll()
                .toList()
                .filter { it.code16 != null && it.code22 != null }
                .associateBy { it.code16!! }
        }
    }
    fun lookUp22Code(iso22Code: String): Iso22? = iso22Categories[iso22Code]

    fun retrieveAll22Iso2(): List<Iso22> = iso22Categories.values.toList()

    fun toCode22(code16: String): String? = isoMaps[code16]?.code22

    fun toIsoMap(code16: String): IsoMap? = isoMaps[code16]
}