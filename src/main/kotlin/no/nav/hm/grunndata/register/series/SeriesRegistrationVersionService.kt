package no.nav.hm.grunndata.register.series

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus

@Singleton
class SeriesRegistrationVersionService(private val seriesRegistrationVersionRepository: SeriesRegistrationVersionRepository,
                                       private val objectMapper: ObjectMapper)
{

    suspend fun findLastApprovedVersion(): SeriesRegistrationVersion? {
        return seriesRegistrationVersionRepository.findOneByDraftStatusAndAdminStatusOrderByVersionDesc(DraftStatus.DONE, AdminStatus.APPROVED)
    }

    suspend fun save(seriesRegistrationVersion: SeriesRegistrationVersion): SeriesRegistrationVersion {
        return seriesRegistrationVersionRepository.save(seriesRegistrationVersion)
    }

    suspend fun update(seriesRegistrationVersion: SeriesRegistrationVersion): SeriesRegistrationVersion {
        return seriesRegistrationVersionRepository.update(seriesRegistrationVersion)
    }

}
