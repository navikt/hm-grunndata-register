package no.nav.hm.grunndata.register.techlabel

import io.micronaut.data.model.Pageable
import io.micronaut.data.repository.jpa.criteria.PredicateSpecification
import jakarta.inject.Singleton

import java.util.*


@Singleton
class TechLabelRegistrationService(private val techLabelRegistrationRepository: TechLabelRegistrationRepository) {

    suspend fun findById(id: UUID) = techLabelRegistrationRepository.findById(id)?.toDTO()
    suspend fun findAll(spec:  PredicateSpecification<TechLabelRegistration>? = null, pageable: Pageable) =
        techLabelRegistrationRepository.findAll(spec, pageable)

    suspend fun save(dto: TechLabelRegistrationDTO) = techLabelRegistrationRepository.save(dto.toEntity()).toDTO()

    suspend fun update(dto: TechLabelRegistrationDTO) = techLabelRegistrationRepository.update(dto.toEntity()).toDTO()

}