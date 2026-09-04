package no.nav.hm.grunndata.register.iso.v22

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

@Singleton
class IsoMapper(private val isoMapRepository: IsoMapRepository   ) {

    var isoMaps: Map<String, IsoMap> = emptyMap()

    init {
        runBlocking {
            isoMaps = isoMapRepository.findAll()
                .toList()
                .filter { it.code16 != null && it.code22 != null }
                .associateBy { it.code16!! }
        }
        LOG.info("Found ${isoMaps.size} isomaps")
    }

    fun mapIso16To22(code16: String): IsoMap? {
        var code16Prefix = code16
        for (code16PrefixLength in code16Prefix.length downTo 2) {
            if (isoMaps[code16Prefix] != null) {
                LOG.info("Found mapping for code16 prefix: $code16Prefix")
                return isoMaps[code16Prefix]
            }
            code16Prefix = code16Prefix.dropLast(2)
        }
        //LOG.error("Could not find mapping for code16: $code16")
        return null
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(IsoMapper::class.java)
    }

}