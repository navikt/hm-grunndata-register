package no.nav.hm.grunndata.register.supplier

import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.SupplierDTO
import no.nav.hm.grunndata.rapid.dto.SupplierInfo
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.rapid.RegisterRapidPushService


@Singleton
class SupplierRegistrationHandler(private val registerRapidPushService: RegisterRapidPushService) {

    fun pushToRapid(dto: SupplierRegistrationDTO, extraKeyValues: Map<String, Any> = emptyMap()) {
        runBlocking {
            if (dto.draftStatus == DraftStatus.DONE) {
                val rapidDTO = dto.toRapidDTO()
                registerRapidPushService.pushDTOToKafka(rapidDTO, EventName.registeredSupplierV1, extraKeyValues)
            }
        }
    }

}

fun SupplierRegistrationDTO.toRapidDTO(): SupplierDTO = SupplierDTO (
    id = id, status = status, name=name, info = supplierData.toInfo() , identifier = identifier, created = created, updated = updated,
    createdBy = createdBy, updatedBy = updatedBy)


private fun SupplierData.toInfo(): SupplierInfo = SupplierInfo (
    address = address, postNr = postNr, postLocation = postLocation, countryCode = countryCode, email = email,
    phone = phone, homepage = homepage
)
