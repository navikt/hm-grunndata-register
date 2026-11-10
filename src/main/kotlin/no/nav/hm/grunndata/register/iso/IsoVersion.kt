package no.nav.hm.grunndata.register.iso

import no.nav.hm.grunndata.register.techlabel.LabelService
import no.nav.hm.grunndata.register.techlabel.TechLabelDTO

enum class IsoVersion {
    v16, v22;

    fun labels(service: LabelService, code16: String, code22: String?): List<TechLabelDTO> =
        when (this) {
            v16 -> service.fetchLabelsByIsoCode(code16)
            v22 -> code22?.takeIf { it.isNotBlank() }?.let { service.fetchLabelsByIsoCode22(it) } ?: emptyList()
        }
}
