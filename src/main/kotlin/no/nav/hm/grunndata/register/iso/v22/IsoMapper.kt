package no.nav.hm.grunndata.register.iso.v22

import jakarta.inject.Singleton
import no.nav.hm.grunndata.register.iso.IsoCategoryService
import org.slf4j.LoggerFactory
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

@Singleton
class IsoMapper( private val isoCategoryService: IsoCategoryService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(IsoMapper::class.java)
    }

}