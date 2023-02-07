package no.nav.hm.grunndata.register.product

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.eventName
import no.nav.hm.rapids_rivers.micronaut.KafkaRapidService
import no.nav.hm.rapids_rivers.micronaut.RapidPushService

@Singleton
open class ProductRegistrationRapidHandler(private val kafkaRapidService: RapidPushService) {

    fun pushProductToKafka(dto: ProductRegistrationDTO) {
        if (dto.draftStatus == DraftStatus.DONE) {
            kafkaRapidService.pushToRapid(
                key = "$eventName-${dto.productDTO.id}",
                eventName = eventName, payload = dto.productDTO
            )
        }
    }
}