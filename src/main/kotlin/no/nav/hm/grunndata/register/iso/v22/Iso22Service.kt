package no.nav.hm.grunndata.register.iso.v22

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.IsoCategory22DTO
import org.slf4j.LoggerFactory

/**
 * Cachen brukes ved serie-mapping (lookUpCode). Appen kjører på flere pods, og admin-endringer skrives bare
 * gjennom til cachen på poden som tok imot dem. Derfor:
 * - retrieveAll leser alltid fra databasen, slik at admin-listen er lik på alle pods.
 * - lookUpCode slår opp i databasen ved bom, slik at nye koder blir funnet med én gang. Bom caches ikke.
 * - refresh kjøres periodisk på alle pods for å plukke opp endret tittel/tekst fra andre pods.
 */
@Singleton
class Iso22Service(
    private val iso22Repository: Iso22Repository,
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(Iso22Service::class.java)
    }

    @Volatile
    private var iso22Categories: Map<String, IsoCategory22DTO> = emptyMap()

    init {
        runBlocking { refresh() }
    }

    suspend fun lookUpCode(isocode: String): IsoCategory22DTO? =
        iso22Categories[isocode]
            ?: iso22Repository.findByIsoCode(isocode)?.toRapidDTO()?.also {
                LOG.debug("Iso22 cache miss for $isocode, loaded from database")
                upsert(it)
            }

    suspend fun retrieveAll(): List<IsoCategory22DTO> = iso22Repository.findAll().map { it.toRapidDTO() }.toList()

    fun upsert(category: IsoCategory22DTO) {
        synchronized(this) {
            iso22Categories = iso22Categories + (category.isoCode to category)
        }
    }

    suspend fun refresh() {
        val fresh = iso22Repository.findAll().map { it.toRapidDTO() }.toList().associateBy { it.isoCode }
        synchronized(this) {
            iso22Categories = fresh
        }
        LOG.info("Refreshed iso22 cache with ${fresh.size} categories")
    }
}
