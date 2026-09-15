package no.nav.hm.grunndata.register.techlabel

interface LabelService {

    fun fetchLabelsByIsoCode(isocode: String): List<TechLabelDTO>
    fun fetchLabelsByIsoCode22(isocode22: String): List<TechLabelDTO>

    fun fetchLabelsByName(name: String): List<TechLabelDTO>?

    fun fetchAllLabels(): Map<String, List<TechLabelDTO>>

    fun fetchAllLabels22(): Map<String, List<TechLabelDTO>>

    fun fetchUnits(): List<String>

    fun fetchLabelNames(): List<String>
}