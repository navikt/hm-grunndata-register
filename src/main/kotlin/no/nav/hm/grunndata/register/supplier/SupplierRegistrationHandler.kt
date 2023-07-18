package no.nav.hm.grunndata.register.supplier

import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.event.EventName
import java.awt.SystemColor

class SupplierRegistrationHandler {

    fun pushToRapidIfNotDraft(dto: SupplierRegistrationDTO) {
        runBlocking {
            if (dto.status == DraftStatus.DONE) {
                val rapidDTO = dto.toRapidDTO()
                registerRapidPushService.pushDTOToKafka(rapidDTO, EventName.registeredProductV1)
            }
        }
    }

}

fun SupplierRegistrationDTO.toRapidDTO(): SupplierDTO = SupplierDTO (
    id = id, status = status, name=name, info = SystemColor.info, identifier = identifier, created = created, updated = updated,
    createdBy = createdBy, updatedBy = updatedBy)
