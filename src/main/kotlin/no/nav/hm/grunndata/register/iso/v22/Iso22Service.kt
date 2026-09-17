package no.nav.hm.grunndata.register.iso.v22


import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.IsoCategory22DTO


@Singleton
class Iso22Service(
    private val iso22Repository: Iso22Repository,
) {

    private var iso22Categories: Map<String, IsoCategory22DTO> = emptyMap()

    init {
        runBlocking {
            iso22Categories = iso22Repository.findAll().map { it.toRapidDTO() }.toList().associateBy { it.isoCode }
        }
    }

    fun lookUpCode(isocode: String): IsoCategory22DTO? = iso22Categories[isocode]

    fun retrieveAll(): List<IsoCategory22DTO> = iso22Categories.values.toList()

}