package no.nav.hm.grunndata.register.iso.v22

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

@Singleton
@CacheConfig(cacheNames = ["isomap"])
open class IsoMapService(private val isoMapRepository: IsoMapRepository) {

    @Cacheable("isomap-all")
    open fun retrieveAll(): List<IsoMapDTO> = runBlocking { isoMapRepository.findAll().filter { it.verified }.toList().map { it.toDTO() } }


    fun mapIso16To22(code16: String): IsoMapDTO? {
        val isoMaps = retrieveAll().filter {it.code16 != null && it.code22 != null }.associateBy { it.code16!! }
        var code16Prefix = code16
        for (code16PrefixLength in code16Prefix.length downTo 2) {
            if (isoMaps[code16Prefix] != null) {
                return isoMaps[code16Prefix]
            }
            code16Prefix = code16Prefix.dropLast(2)
        }
        return null
    }
}