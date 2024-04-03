package no.nav.hm.grunndata.register.iso

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.IsoCategoryDTO
import org.slf4j.LoggerFactory

@Singleton
class IsoCategoryService(
    private val isoCategoryRegistrationRepository: IsoCategoryRegistrationRepository
) {

    private var isoCategories: Map<String, IsoCategoryDTO>


    companion object {
        private val LOG = LoggerFactory.getLogger(IsoCategoryService::class.java)
    }

    init {
        runBlocking {
            isoCategories =
                isoCategoryRegistrationRepository.findAll().map { it.toRapidDTO() }.toList().associateBy { it.isoCode }
            if (isoCategories.size < 1000) {
                LOG.error("ISO categories are not loaded properly, only ${isoCategories.size} loaded")
            }
        }

    }

    fun lookUpCode(isoCode: String): IsoCategoryDTO? = isoCategories[isoCode]

    fun retrieveAllCategories(): List<IsoCategoryDTO> = isoCategories.values.toList()

}
