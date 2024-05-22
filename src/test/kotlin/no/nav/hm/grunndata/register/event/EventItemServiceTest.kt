package no.nav.hm.grunndata.register.event

import io.kotest.common.runBlocking
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import no.nav.hm.grunndata.rapid.dto.AgreementPost
import no.nav.hm.grunndata.rapid.event.EventName
import no.nav.hm.grunndata.register.agreement.AgreementData
import no.nav.hm.grunndata.register.agreement.AgreementRegistrationDTO
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

@MicronautTest
class EventItemServiceTest(private val eventItemService: EventItemService) {

    @Test
    fun testEventItemService() {
        runBlocking {
            val agreementId1 = UUID.randomUUID()
            val agreementId2 = UUID.randomUUID()

            val data = AgreementData(
                text = "some text",
                resume = "resume",
                identifier = UUID.randomUUID().toString(),
                posts = listOf(
                    AgreementPost(
                        identifier = "unik-post1", title = "Post title",
                        description = "post description", nr = 1
                    ), AgreementPost(
                        identifier = "unik-post2", title = "Post title 2",
                        description = "post description 2", nr = 2
                    )
                )
            )

            val agreementRegistration = AgreementRegistrationDTO(
                id = agreementId1,
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(2),
                title = "Rammeavtale 1",
                reference = "unik-ref4",
                updatedByUser = "test",
                createdByUser = "test",
                agreementData = data,
                publicationDate = LocalDateTime.now().plusDays(5)
            )

            val data2 = AgreementData(
                text = "some text",
                resume = "resume",
                identifier = UUID.randomUUID().toString(),
                posts = listOf(
                    AgreementPost(
                        identifier = "unik-post1", title = "Post title",
                        description = "post description", nr = 1
                    ), AgreementPost(
                        identifier = "unik-post2", title = "Post title 2",
                        description = "post description 2", nr = 2
                    )
                )
            )

            val agreementRegistration2 = AgreementRegistrationDTO(
                id = agreementId2,
                published = LocalDateTime.now(),
                expired = LocalDateTime.now().plusYears(2),
                title = "Rammeavtale 2",
                reference = "unik-ref5",
                updatedByUser = "test2",
                createdByUser = "test2",
                agreementData = data2
            )

            eventItemService.createNewEventItem(
                type = EventItemType.AGREEMENT,
                oid = agreementId1,
                byUser = "test",
                eventName = EventName.registeredAgreementV1,
                payload = agreementRegistration,
                extraKeyValues = emptyMap()
            )

            eventItemService.createNewEventItem(
                type = EventItemType.AGREEMENT,
                oid = agreementId2,
                byUser = "test2",
                eventName = EventName.registeredAgreementV1,
                payload = agreementRegistration2,
                extraKeyValues = emptyMap()
            )
            val items = eventItemService.getAllPendingStatus()
            items.size shouldBeGreaterThanOrEqual 1
            items.forEach {
                it.status shouldBe EventItemStatus.PENDING
                it.publicationDate should shouldBeNullOrBefore(LocalDateTime.now())
            }
        }
    }
}

fun shouldBeNullOrBefore(date: LocalDateTime): Matcher<LocalDateTime?> = object : Matcher<LocalDateTime?> {
    override fun test(value: LocalDateTime?): MatcherResult {
        val passed = value == null || value.isBefore(date)
        return MatcherResult(
            passed,
            { "Date $value should be null or after $date" },
            { "Date $value should not be null or after $date" }
        )
    }
}

