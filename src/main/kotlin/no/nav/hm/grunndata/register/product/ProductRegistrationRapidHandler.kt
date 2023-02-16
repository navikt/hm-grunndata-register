package no.nav.hm.grunndata.register.product

import jakarta.inject.Singleton
import no.nav.hm.grunndata.dto.AdminStatus
import no.nav.hm.grunndata.dto.DraftStatus
import no.nav.hm.grunndata.dto.ProductRegistrationDTO
import no.nav.hm.grunndata.register.productRegistrationEventName
import no.nav.hm.rapids_rivers.micronaut.RapidPushService

@Singleton
open class ProductRegistrationRapidHandler(private val kafkaRapidService: RapidPushService) {

    fun pushProductToKafka(dto: ProductRegistrationDTO) {
        if (dto.draftStatus == DraftStatus.DONE && dto.adminStatus == AdminStatus.APPROVED) {
            kafkaRapidService.pushToRapid(
                key = "$productRegistrationEventName-${dto.id}",
                eventName = productRegistrationEventName, payload = dto
            )
        }
    }
}