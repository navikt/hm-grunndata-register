package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.version.DiffStatus
import no.nav.hm.grunndata.register.version.Difference
import no.nav.hm.grunndata.register.version.MapDifference
import no.nav.hm.grunndata.register.version.difference

@Singleton
class SeriesRegistrationVersionService(private val seriesRegistrationVersionRepository: SeriesRegistrationVersionRepository,
                                       private val objectMapper: ObjectMapper)
{

    suspend fun findLastApprovedVersion(): SeriesRegistrationVersion? {
        return seriesRegistrationVersionRepository.findOneByDraftStatusAndAdminStatusOrderByUpdatedDesc(DraftStatus.DONE, AdminStatus.APPROVED)
    }

    suspend fun save(seriesRegistrationVersion: SeriesRegistrationVersion): SeriesRegistrationVersion {
        return seriesRegistrationVersionRepository.save(seriesRegistrationVersion)
    }

    suspend fun update(seriesRegistrationVersion: SeriesRegistrationVersion): SeriesRegistrationVersion {
        return seriesRegistrationVersionRepository.update(seriesRegistrationVersion)
    }

    fun <K, V> diffVersions(version1: SeriesRegistrationVersion, version2: SeriesRegistrationVersion): Difference<K,V> {
        val version1Map: Map<K,V> = objectMapper.convertValue(version1.seriesRegistration)
        val version2Map: Map<K,V> = objectMapper.convertValue(version2.seriesRegistration)
        return version1Map.difference(version2Map)
    }

    fun <K, V> diffVersions(seriesRegistrationDTO: SeriesRegistrationDTO, version2: SeriesRegistrationVersion): Difference<K,V> {
        val version1Map: Map<K,V> = objectMapper.convertValue(seriesRegistrationDTO)
        val version2Map: Map<K,V> = objectMapper.convertValue(version2.seriesRegistration)
        return version1Map.difference(version2Map)
    }

    fun <K, V> diffVersions(seriesRegistrationDTO1: SeriesRegistrationDTO, seriesRegistrationDTO2: SeriesRegistrationDTO): Difference<K, V> {
        val version1Map: Map<K, V> = objectMapper.convertValue(seriesRegistrationDTO1)
        val version2Map: Map<K, V> = objectMapper.convertValue(seriesRegistrationDTO2)
        return version1Map.difference(version2Map)
    }

    suspend fun <K, V> diffWithLastApprovedVersion(seriesRegistrationDTO: SeriesRegistrationDTO): Difference<K, V> {
        val lastApprovedVersion = findLastApprovedVersion()
        return if (lastApprovedVersion != null) {
            diffVersions(seriesRegistrationDTO, lastApprovedVersion)
        } else {
            Difference(DiffStatus.NEW, MapDifference())
        }
    }
}
