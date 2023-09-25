package no.nav.hm.grunndata.register.series

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.data.model.Page
import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import kotlinx.coroutines.flow.map
import no.nav.hm.grunndata.register.product.toDTO
import java.util.*

@Singleton
@CacheConfig("series")
open class SeriesRegistrationService(private val seriesRegistrationRepository: SeriesRegistrationRepository) {

    @Cacheable
    open suspend fun findById(id: UUID): SeriesRegistrationDTO? = seriesRegistrationRepository.findById(id)?.toDTO()


    @CacheInvalidate(parameters = ["id"])
    open suspend fun update(dto: SeriesRegistrationDTO, id: UUID = dto.id) =
        seriesRegistrationRepository.update(dto.toEntity()).toDTO()

    @CacheInvalidate(parameters = ["id"])
    open suspend fun save(dto: SeriesRegistrationDTO, id: UUID = dto.id) =
        seriesRegistrationRepository.save(dto.toEntity()).toDTO()

    @Transactional
    open suspend fun saveAndPushToRapidIfNotDraft(dto: SeriesRegistrationDTO, isUpdate: Boolean): SeriesRegistrationDTO {
        return if (isUpdate) update(dto) else save(dto)
    }

    open suspend fun findAll(spec: PredicateSpecification<SeriesRegistration>?, pageable: Pageable): Page<SeriesRegistrationDTO> =
        seriesRegistrationRepository.findAll(spec, pageable).map { it.toDTO() }

}
