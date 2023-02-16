package no.nav.hm.grunndata.register.product

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.AdminStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.ProductRegistrationDTO
import no.nav.hm.rapids_rivers.micronaut.RapidPushService

@Singleton
open class ProductRegistrationRapidHandler(private val kafkaRapidService: RapidPushService) {

}