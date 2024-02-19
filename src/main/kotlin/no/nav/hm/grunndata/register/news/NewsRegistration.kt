package no.nav.hm.grunndata.register.news

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.rapid.dto.NewsRegistrationRapidDTO
import no.nav.hm.grunndata.rapid.dto.NewsStatus
import no.nav.hm.grunndata.register.event.EventPayload
import java.time.LocalDateTime
import java.util.*

@MappedEntity("news_reg_v1")
data class NewsRegistration(
    @field:Id
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val text: String,
    val status: NewsStatus = NewsStatus.ACTIVE,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusMonths(3),
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val author: String = "Admin",
    val createdBy: String,
    val updatedBy: String,
    val createdByUser: String,
    val updatedByUser: String
)

data class NewsRegistrationDTO(
    override val id: UUID = UUID.randomUUID(),
    val title: String,
    val text: String,
    val status: NewsStatus = NewsStatus.ACTIVE,
    val draftStatus: DraftStatus = DraftStatus.DRAFT,
    val published: LocalDateTime = LocalDateTime.now(),
    val expired: LocalDateTime = LocalDateTime.now().plusMonths(3),
    val created: LocalDateTime = LocalDateTime.now(),
    val updated: LocalDateTime = LocalDateTime.now(),
    val author: String = "Admin",
    val createdBy: String,
    val updatedBy: String,
    val createdByUser: String,
    override val updatedByUser: String
): EventPayload {
    override fun toRapidDTO(): NewsRegistrationRapidDTO = NewsRegistrationRapidDTO(
        id = id,
        title = title,
        text = text,
        status = status,
        draftStatus = draftStatus,
        published = published,
        expired = expired,
        created = created,
        updated = updated,
        author = author,
        createdBy = createdBy,
        updatedBy = updatedBy,
        createdByUser = createdByUser,
        updatedByUser = updatedByUser
    )
}



fun NewsRegistration.toDTO(): NewsRegistrationDTO = NewsRegistrationDTO(
    id = id,
    title = title,
    text = text,
    status = status,
    draftStatus = draftStatus,
    published = published,
    expired = expired,
    created = created,
    updated = updated,
    author = author,
    updatedBy = updatedBy,
    createdBy = createdBy,
    createdByUser = createdByUser,
    updatedByUser = updatedByUser
)

fun NewsRegistrationDTO.toEntity(): NewsRegistration = NewsRegistration(
    id = id,
    title = title,
    text = text,
    status = status,
    draftStatus = draftStatus,
    published = published,
    expired = expired,
    created = created,
    updated = updated,
    author = author,
    updatedBy = updatedBy,
    createdBy = createdBy,
    createdByUser = createdByUser,
    updatedByUser = updatedByUser
)

