package no.nav.hm.grunndata.register.techlabel

interface LabelService {

    fun fetchLabelsByIsoCode(isocode: String): List<TechLabelDTO>
    fun fetchLabelsByName(name: String): List<TechLabelDTO>?

    fun fetchAllLabels(): Map<String, List<TechLabelDTO>>


}