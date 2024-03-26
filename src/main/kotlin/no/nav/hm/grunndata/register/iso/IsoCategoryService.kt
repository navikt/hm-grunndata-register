package no.nav.hm.grunndata.register.iso

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.IsoCategoryDTO
import no.nav.hm.grunndata.register.HMDB
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import org.slf4j.LoggerFactory

@Singleton
class IsoCategoryService(
    private val gdbApiClient: GdbApiClient,
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
            if (isoCategories.isEmpty()) {
                val categories = gdbApiClient.retrieveIsoCategories()
                isoCategories = categories.associateBy { it.isoCode }
                isoCategories.values.forEach {
                    isoCategoryRegistrationRepository.save(
                        IsoCategoryRegistration(
                            isoCode = it.isoCode,
                            isoTitle = it.isoTitle,
                            isoText = it.isoText,
                            isoTranslations = IsoTranslations(
                                titleEn = it.isoTranslations?.titleEn,
                                textEn = it.isoTranslations?.textEn
                            ),
                            isoTextShort = it.isoTextShort ?: "",
                            isoLevel = it.isoLevel,
                            isActive = it.isActive,
                            showTech = it.showTech,
                            allowMulti = it.allowMulti,
                            createdByUser = HMDB,
                            updatedByUser = HMDB,
                            createdBy = HMDB,
                            updatedBy = HMDB
                        )
                    )
                }
            }
        }
        LOG.info("Got isoCategories: ${isoCategories.size}")
    }

    fun lookUpCode(isoCode: String): IsoCategoryDTO? = isoCategories[isoCode]

    fun retrieveAllCategories(): List<IsoCategoryDTO> = isoCategories.values.toList()

}
