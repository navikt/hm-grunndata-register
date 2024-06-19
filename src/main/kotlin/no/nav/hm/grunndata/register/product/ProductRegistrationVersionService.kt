package no.nav.hm.grunndata.register.product

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
class ProductRegistrationVersionService(private val productRegistrationVersionRepository: ProductRegistrationVersionRepository,
                                        private val objectMapper: ObjectMapper) {

    companion object {
        private val LOG = LoggerFactory.getLogger(ProductRegistrationVersionService::class.java)
    }

    suspend fun findLastApprovedVersion(productId: UUID): ProductRegistrationVersion? {
        return productRegistrationVersionRepository.findOneByProductIdAndDraftStatusAndAdminStatusOrderByUpdatedDesc(
            productId, DraftStatus.DONE, AdminStatus.APPROVED)
    }

    suspend fun save(productRegistrationVersion: ProductRegistrationVersion): ProductRegistrationVersion {
        return productRegistrationVersionRepository.save(productRegistrationVersion)
    }

    suspend fun update(productRegistrationVersion: ProductRegistrationVersion): ProductRegistrationVersion {
        return productRegistrationVersionRepository.update(productRegistrationVersion)
    }

    suspend fun deleteOldVersions() {
        val draftsOlderThan1Month = productRegistrationVersionRepository
            .findByDraftStatusAndUpdatedBefore(DraftStatus.DRAFT, LocalDateTime.now().minusMonths(1))
        LOG.info("Deleting ${draftsOlderThan1Month.size} draft product versions older than 1 month")
        draftsOlderThan1Month.forEach {
            productRegistrationVersionRepository.delete(it)
        }
        val olderThan1Year = productRegistrationVersionRepository
            .findByDraftStatusAndUpdatedBefore(DraftStatus.DONE, LocalDateTime.now().minusYears(1))
        LOG.info("Deleting ${olderThan1Year.size} product versions older than 1 year")
        olderThan1Year.forEach {
            productRegistrationVersionRepository.delete(it)
        }
    }

    fun <K,V> diffVersions(version1: ProductRegistrationVersion, version2: ProductRegistrationVersion): Difference<K, V> {
        val version1Map: Map<K,V> = objectMapper.convertValue(version1.productRegistration)
        val version2Map: Map<K,V> = objectMapper.convertValue(version2.productRegistration)
        return version1Map.difference(version2Map)
    }

    fun <K,V> diffVersions(productRegistration: ProductRegistration, version2: ProductRegistrationVersion): Difference<K, V> {
        val version1Map: Map<K,V> = objectMapper.convertValue(productRegistration)
        val version2Map: Map<K,V> = objectMapper.convertValue(version2.productRegistration)
        return version1Map.difference(version2Map)
    }

    fun <K,V> diffVersions(productRegistration: ProductRegistration, productRegistration2: ProductRegistration): Difference<K, V> {
        val version1Map: Map<K,V> = objectMapper.convertValue(productRegistration)
        val version2Map: Map<K,V> = objectMapper.convertValue(productRegistration2)
        return version1Map.difference(version2Map)
    }

    suspend fun <K,V> diffWithLastApprovedVersion(productRegistration: ProductRegistration): Difference<K, V> {
        val lastApprovedVersion = findLastApprovedVersion(productRegistration.id)
        return if (lastApprovedVersion != null) {
            diffVersions(productRegistration, lastApprovedVersion)
        } else {
            Difference(DiffStatus.NEW, MapDifference())
        }
    }

    suspend fun findAll(spec: PredicateSpecification<ProductRegistrationVersion>?, pageable: Pageable): Page<ProductRegistrationVersion>  =
        productRegistrationVersionRepository.findAll(spec, pageable)

    suspend fun findByProductIdAndVersion(productId: UUID, version: Long): ProductRegistrationVersion? =
        productRegistrationVersionRepository.findByProductIdAndVersion(productId, version)

}