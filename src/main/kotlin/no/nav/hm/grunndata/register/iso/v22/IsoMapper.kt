package no.nav.hm.grunndata.register.iso.v22

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import org.slf4j.LoggerFactory
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

@Singleton
class IsoMapper(private val objectMapper: ObjectMapper, private val isoCategoryService: IsoCategoryService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(IsoMapper::class.java)
    }

    var kode16Map: List<Kode16Map>
    var iso16To2Map: Map<String, Kode16Map>

    init {
        kode16Map = IsoMapper::class.java.getResourceAsStream("/iso/isomap_16-22.json")?.use { inputStream ->
        objectMapper.readValue(inputStream, object: TypeReference<List<Kode16Map>>() {}).map {
            val paddedKode16 = if (it.kode16 != null && it.kode16.length % 2 != 0) it.kode16.padStart( it.kode16.length+1,'0') else it.kode16
            val paddedKode22 = if (it.kode22 != null && it.kode22.length % 2 != 0) it.kode22.padStart(it.kode22.length+1,'0') else it.kode22
            it.copy(kode16 = paddedKode16, kode22 = paddedKode22)
        }
        } ?: run {
            LOG.error("Could not load iso-mapping.json")
            emptyList()
        }
        iso16To2Map = kode16Map.filter { it.kode16 != null }.associateBy { it.kode16!! }
    }

    fun getIso16ToIso22(kode16: String): Kode16Map? = iso16To2Map[kode16]

}