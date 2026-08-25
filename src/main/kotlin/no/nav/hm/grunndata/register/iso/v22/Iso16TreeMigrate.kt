package no.nav.hm.grunndata.register.iso.v22

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import org.slf4j.LoggerFactory

@Singleton
class Iso16TreeMigrate(private val isoCategoryService: IsoCategoryService,
                       private val isoMapper: IsoMapper) {

    fun migrateIso16Tree(): List<IsoMapResult>  {
        val iso16Categories = isoCategoryService.retrieveAllCategories()
        val isoMaps =  iso16Categories.map { category ->
            isoMapper.mapIso16To22(category.isoCode)?.let { isoMap ->
                LOG.debug("Found mapping for iso16: ${category.isoCode} to iso22: ${isoMap.code22} with enum ${isoMap.mapEnum}")
                IsoMapResult(
                    code16 = category.isoCode,
                    code22 = isoMap.code22,
                    isoMap = isoMap
                )
            } ?: run {
                LOG.error("Could not find mapping for iso16: ${category.isoCode} with name: ${category.isoTitle}")
                IsoMapResult(
                    code16 = category.isoCode,
                    code22 = null,
                    isoMap = null
                )

            }
        }
        return isoMaps
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Iso16TreeMigrate::class.java)
    }

}

data class IsoMapResult(
    val code16: String,
    val code22: String?,
    val isoMap: IsoMap? = null
)