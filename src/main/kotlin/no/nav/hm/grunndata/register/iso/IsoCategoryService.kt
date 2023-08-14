package no.nav.hm.grunndata.register.iso

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.IsoCategoryDTO
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import org.slf4j.LoggerFactory

@Singleton
class IsoCategoryService(gdbApiClient: GdbApiClient) {

    private val isoCategories: Map<String, IsoCategoryDTO> =
        gdbApiClient.retrieveIsoCategories().associateBy { it.isoCode }

    companion object {
        private val LOG = LoggerFactory.getLogger(IsoCategoryService::class.java)
    }

    init {
        LOG.info("Got isoCategories: ${isoCategories.size}")
    }

    fun lookUpCode(isoCode: String): IsoCategoryDTO? = isoCategories[isoCode]

    fun retrieveAllCategories(): List<IsoCategoryDTO> = isoCategories.values.toList()

}
