package no.nav.hm.grunndata.register.iso

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.iso.v22.IsoMapService
import no.nav.hm.grunndata.register.series.SeriesRegistration
import org.slf4j.LoggerFactory

@Singleton
class SeriesIsoMapper(private val mappings: IsoMapService) {
    fun hasWarning(series: SeriesRegistration, version: IsoVersion): Boolean = when (version) {
        IsoVersion.v16 -> mappings.mapIso16To22Lvl4(series.isoCategory)?.let { it != series.isoCategory22 } ?: true
        IsoVersion.v22 -> series.isoCategory22?.let { code ->
            mappings.mapIso22To16Lvl4(code)?.let { it != series.isoCategory } ?: true
        } ?: true
    }

    fun update(series: SeriesRegistration, code: String, version: IsoVersion): SeriesRegistration {
        val counterpart = when (version) {
            IsoVersion.v16 -> mappings.mapIso16To22Lvl4(code)
            IsoVersion.v22 -> mappings.mapIso22To16Lvl4(code)
        }
        if (counterpart.isNullOrBlank()) {
            LOG.warn("Cannot maintain ISO counterpart for {} category {}; retaining existing counterpart", version, code)
        }
        return when (version) {
            IsoVersion.v16 -> series.copy(isoCategory = code, isoCategory22 = counterpart ?: series.isoCategory22)
            IsoVersion.v22 -> series.copy(isoCategory22 = code, isoCategory = counterpart ?: series.isoCategory)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesIsoMapper::class.java)
    }
}
