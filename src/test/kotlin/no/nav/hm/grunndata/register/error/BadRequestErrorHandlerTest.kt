package no.nav.hm.grunndata.register.error

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class BadRequestErrorHandlerTest {
    @Test
    fun `should sanitize logged errors without changing the response`() {
        val logger = LoggerFactory.getLogger("HttpRequestErrorHandler") as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        val message = "Invalid value\r\nERROR forged-event"

        try {
            val response = BadRequestErrorHandler().handle(
                HttpRequest.GET<Any>("/"),
                BadRequestException(message),
            )

            response.status shouldBe HttpStatus.BAD_REQUEST
            response.body()!!.message shouldBe message
            appender.list.single().formattedMessage shouldBe
                response.body().toString().replace("\r\n", "__")
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }
}
