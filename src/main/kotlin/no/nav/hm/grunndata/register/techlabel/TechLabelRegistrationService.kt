package no.nav.hm.grunndata.register.techlabel

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map
import java.util.*


@Singleton
class TechLabelRegistrationService(private val techLabelRegistrationRepository: TechLabelRegistrationRepository) {

    suspend fun findById(id: UUID) = techLabelRegistrationRepository.findById(id)?.toDTO()
    suspend fun findAll() = techLabelRegistrationRepository.findAll().map { it.toDTO() }

    suspend fun save(dto: TechLabelRegistrationDTO) = techLabelRegistrationRepository.save(dto.toEntity()).toDTO()

    suspend fun update(dto: TechLabelRegistrationDTO) = techLabelRegistrationRepository.update(dto.toEntity()).toDTO()

}