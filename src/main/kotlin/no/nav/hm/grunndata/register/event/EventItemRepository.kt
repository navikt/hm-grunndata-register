package no.nav.hm.grunndata.register.event

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime
import java.util.*


@JdbcRepository(dialect = Dialect.POSTGRES)
interface EventItemRepository:CoroutineCrudRepository<EventItem, UUID>  {

    suspend fun findByStatus(status: EventItemStatus): List<EventItem>

    suspend fun deleteByStatusAndUpdatedBefore(status: EventItemStatus, updated: LocalDateTime): Int

    suspend fun findByStatusAndPublicationDateBeforeOrPublicationDateIsNull(status: EventItemStatus, publicationDate: LocalDateTime): List<EventItem>

}