package no.nav.hm.grunndata.register.iso

import jakarta.inject.Singleton
import kotlinx.coroutines.flow.map


@Singleton
class IsoCategoryRegistrationService(private val IsoCategoryRegistrationRepository: IsoCategoryRegistrationRepository)  {


    suspend fun findByCode(code: String) = IsoCategoryRegistrationRepository.findById(code)?.toDTO()

    suspend fun findAll() = IsoCategoryRegistrationRepository.findAll().map { it.toDTO() }

    suspend fun save(dto: IsoCategoryRegistrationDTO) = IsoCategoryRegistrationRepository.save(dto.toEntity()).toDTO()

    suspend fun update(dto: IsoCategoryRegistrationDTO) = IsoCategoryRegistrationRepository.update(dto.toEntity()).toDTO()

}


