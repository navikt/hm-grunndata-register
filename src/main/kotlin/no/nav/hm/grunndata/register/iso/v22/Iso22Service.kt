package no.nav.hm.grunndata.register.iso.v22


import jakarta.inject.Singleton
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import org.slf4j.LoggerFactory

@Singleton
class Iso22Service(
    private val iso22Repository: Iso22Repository,
    private val iso16ToIso22Util: Iso16ToIso22Util,
) {

    private var iso22Categories: Map<String, Iso22>

    companion object {
        private val LOG = LoggerFactory.getLogger(IsoCategoryService::class.java)
    }

    init {
        runBlocking {
            iso22Categories = iso22Repository.findAll().toList().associateBy { it.isoCode }
            //iso16TreeMigrate.migrateIso16Tree()
        }
    }
    fun lookUp22Code(iso22Code: String): Iso22? = iso22Categories[iso22Code]

    fun retrieveAll22Iso2(): List<Iso22> = iso22Categories.values.toList()

}