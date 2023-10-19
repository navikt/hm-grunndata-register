package no.nav.hm.grunndata.register.series

import io.micronaut.security.authentication.Authentication
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.*
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.rapid.RegisterRapidPushService
import no.nav.hm.grunndata.register.security.Roles
import no.nav.hm.grunndata.register.supplier.SupplierRegistrationService
import no.nav.hm.grunndata.register.supplier.toRapidDTO
import java.time.LocalDateTime

@Singleton
class SeriesRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService,
                                private val supplierRegistrationService: SupplierRegistrationService) {
    fun pushToRapidIfNotDraft(dto: SeriesRegistrationDTO, extraKeyValues:Map<String, Any> = emptyMap()) {
        runBlocking {
            if (dto.draftStatus == DraftStatus.DONE) {
                val rapidDTO = dto.toRapidDTO()
                registerRapidPushService.pushDTOToKafka(rapidDTO, EventName.registeredSeriesV1, extraKeyValues)
            }
        }
    }
    private fun SeriesRegistrationDTO.toRapidDTO() = SeriesRegistrationRapidDTO (
        id = id,
        supplierId = supplierId,
        identifier = identifier,
        title = title,
        draftStatus = draftStatus,
        status = status,
        created = created,
        updated = updated,
        expired = expired,
        createdBy = createdBy,
        updatedBy = updatedBy,
        updatedByUser = updatedByUser,
        createdByUser = createdByUser,
        createdByAdmin = createdByAdmin,
        version = version
    )

}

