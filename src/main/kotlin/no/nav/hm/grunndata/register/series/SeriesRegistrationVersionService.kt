package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.util.UUID
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

    suspend fun findLastApprovedVersion(seriesId: UUID): SeriesRegistrationVersionDTO?  {
        return seriesRegistrationVersionRepository.findOneBySeriesIdAndDraftStatusAndAdminStatusOrderByUpdatedDesc(seriesId, DraftStatus.DONE, AdminStatus.APPROVED)?.toDTO()
    }

    suspend fun save(seriesRegistrationVersion: SeriesRegistrationVersion): SeriesRegistrationVersionDTO {
        return seriesRegistrationVersionRepository.save(seriesRegistrationVersion).toDTO()
    }

    suspend fun update(seriesRegistrationVersion: SeriesRegistrationVersion): SeriesRegistrationVersionDTO {
        return seriesRegistrationVersionRepository.update(seriesRegistrationVersion).toDTO()
    }


    suspend fun deleteOldVersions() {
        val draftsOlderThan1Month = seriesRegistrationVersionRepository.findByDraftStatusAndUpdatedBefore(DraftStatus.DRAFT, LocalDateTime.now().minusMonths(1))
        LOG.info("Deleting ${draftsOlderThan1Month.size} draft series versions older than 1 month")
        draftsOlderThan1Month.forEach {
            seriesRegistrationVersionRepository.delete(it)
        }
        val olderThan1Year = seriesRegistrationVersionRepository.findByDraftStatusAndUpdatedBefore(DraftStatus.DONE, LocalDateTime.now().minusYears(1))
        LOG.info("Deleting ${olderThan1Year.size} series versions older than 1 year")
        olderThan1Year.forEach {
            seriesRegistrationVersionRepository.delete(it)
        }
    }

    fun <K, V> diffVersions(version1: SeriesRegistrationVersionDTO, version2: SeriesRegistrationVersionDTO): Difference<K,V> {
        val version1Map: Map<K,V> = objectMapper.convertValue(version1.seriesRegistration)
        val version2Map: Map<K,V> = objectMapper.convertValue(version2.seriesRegistration)
        return version1Map.difference(version2Map)
    }

    fun <K, V> diffVersions(seriesRegistration: SeriesRegistrationDTO, version2: SeriesRegistrationVersionDTO): Difference<K,V> {
        val version1Map: Map<K,V> = objectMapper.convertValue(seriesRegistration)
        val version2Map: Map<K,V> = objectMapper.convertValue(version2.seriesRegistration)
        return version1Map.difference(version2Map)
    }

    fun <K, V> diffVersions(seriesRegistration: SeriesRegistrationDTO, seriesRegistration2: SeriesRegistrationDTO): Difference<K, V> {
        val version1Map: Map<K, V> = objectMapper.convertValue(seriesRegistration)
        val version2Map: Map<K, V> = objectMapper.convertValue(seriesRegistration2)
        return version1Map.difference(version2Map)
    }

    suspend fun <K, V> diffWithLastApprovedVersion(seriesRegistration: SeriesRegistrationDTO): Difference<K, V> {
        val lastApprovedVersion = findLastApprovedVersion(seriesRegistration.id)
        return if (lastApprovedVersion != null) {
            diffVersions(seriesRegistration, lastApprovedVersion)
        } else {
            Difference(DiffStatus.NEW, MapDifference())
        }
    }

    suspend fun findAll(spec: PredicateSpecification<SeriesRegistrationVersion>?, pageable: Pageable): Page<SeriesRegistrationVersionDTO> =
        seriesRegistrationVersionRepository.findAll(spec, pageable).map { it.toDTO() }

    suspend fun findBySeriesIdAndVersion(seriesId: UUID, version: Long): SeriesRegistrationVersionDTO? =
        seriesRegistrationVersionRepository.findBySeriesIdAndVersion(seriesId, version)?.toDTO()



}
