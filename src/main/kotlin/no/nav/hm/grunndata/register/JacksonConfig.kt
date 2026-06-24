package no.nav.hm.grunndata.register

import com.fasterxml.jackson.annotation.JsonInclude

import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Singleton
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper

@Singleton
class JacksonConfig : BeanCreatedEventListener<JsonMapper.Builder> {

    companion object {
        private val LOG = org.slf4j.LoggerFactory.getLogger(JacksonConfig::class.java)
    }

    override fun onCreated(event: BeanCreatedEvent<JsonMapper.Builder>): JsonMapper.Builder {
        LOG.info("Initialized JacksonConfig")
        event.bean
            .changeDefaultPropertyInclusion{it.withValueInclusion(JsonInclude.Include.NON_NULL)}
            .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT, false)
        return event.bean
    }
}
