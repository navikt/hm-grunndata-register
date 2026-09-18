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
    fun retrieveAll(): List<IsoMapDTO> = runBlocking { isoMapRepository.findAll().filter { it.verified }.toList().map { it.toDTO() } }

}