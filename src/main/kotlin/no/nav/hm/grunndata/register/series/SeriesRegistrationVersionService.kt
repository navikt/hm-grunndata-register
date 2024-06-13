package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import jakarta.inject.Singleton
import java.time.LocalDateTime
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.version.DiffStatus
import no.nav.hm.grunndata.register.version.Difference
import no.nav.hm.grunndata.register.version.MapDifference
import no.nav.hm.grunndata.register.version.difference
import org.slf4j.LoggerFactory

@Singleton
class SeriesRegistrationVersionService(private val seriesRegistrationVersionRepository: SeriesRegistrationVersionRepository,
                                       private val objectMapper: ObjectMapper)
{

    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesRegistrationVersionService::class.java)
    }

    suspend fun findLastApprovedVersion(): SeriesRegistrationVersion? {
        return seriesRegistrationVersionRepository.findOneByDraftStatusAndAdminStatusOrderByUpdatedDesc(DraftStatus.DONE, AdminStatus.APPROVED)
    }

    suspend fun save(seriesRegistrationVersion: SeriesRegistrationVersion): SeriesRegistrationVersion {
        return seriesRegistrationVersionRepository.save(seriesRegistrationVersion)
    }

    suspend fun update(seriesRegistrationVersion: SeriesRegistrationVersion): SeriesRegistrationVersion {
        return seriesRegistrationVersionRepository.update(seriesRegistrationVersion)
    }

    suspend fun deleteOldVersions() {
        val draftsOlderThan1Month = seriesRegistrationVersionRepository.findByDraftStatusAndUpdatedBefore(DraftStatus.DRAFT, LocalDateTime.now().minusMonths(1))
        LOG.info("Deleting ${draftsOlderThan1Month.size} draft versions older than 1 month")
        draftsOlderThan1Month.forEach {
            seriesRegistrationVersionRepository.delete(it)
        }
        val olderThan1Year = seriesRegistrationVersionRepository.findByDraftStatusAndUpdatedBefore(DraftStatus.DONE, LocalDateTime.now().minusYears(1))
        LOG.info("Deleting ${olderThan1Year.size} versions older than 1 year")
        olderThan1Year.forEach {
            seriesRegistrationVersionRepository.delete(it)
        }
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