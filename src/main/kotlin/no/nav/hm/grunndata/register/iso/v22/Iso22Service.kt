package no.nav.hm.grunndata.register.iso.v22


import jakarta.inject.Singleton
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking


@Singleton
class Iso22Service(
    private val iso22Repository: Iso22Repository,
) {

    private var iso22Categories: Map<String, Iso22> = emptyMap()

    init {
        runBlocking {
            iso22Categories = iso22Repository.findAll().toList().associateBy { it.isoCode }
        }
    }
    fun lookUp22Code(iso22Code: String): Iso22? = iso22Categories[iso22Code]

    fun retrieveAll22Iso2(): List<Iso22> = iso22Categories.values.toList()

}