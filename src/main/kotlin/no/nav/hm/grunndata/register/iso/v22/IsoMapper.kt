package no.nav.hm.grunndata.register.iso.v22

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import org.slf4j.LoggerFactory
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

@Singleton
class IsoMapper(private val isoCategoryService: IsoCategoryService, private val isoMapRepository: IsoMapRepository) {

    var isoMaps: Map<String, IsoMap> = emptyMap()

    init {
        runBlocking {
            isoMaps =  isoMapRepository.findAll().toList().associateBy { it.code16 }
        }
    }

    fun toCode22(code16: String): String? = isoMaps[code16]?.code22

    fun toIsoMap(code16: String): IsoMap? = isoMaps[code16]

    
    companion object {
        private val LOG = LoggerFactory.getLogger(IsoMapper::class.java)
    }

}