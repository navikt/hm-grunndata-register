package no.nav.hm.grunndata.register.agreement

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.jpa.kotlin.CoroutineJpaSpecificationExecutor
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import no.nav.hm.grunndata.rapid.dto.AgreementStatus
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import java.time.LocalDateTime
import java.util.*

@JdbcRepository(dialect = Dialect.POSTGRES)
interface AgreementRegistrationRepository: CoroutineCrudRepository<AgreementRegistration, UUID>,
    CoroutineJpaSpecificationExecutor<AgreementRegistration> {

        suspend fun findByReference(reference: String): AgreementRegistration?

        suspend fun findByDraftStatusAndAgreementStatusAndExpiredBefore(draftStatus: DraftStatus = DraftStatus.DONE, status: AgreementStatus, expired: LocalDateTime? = LocalDateTime.now()): List<AgreementRegistration>

        suspend fun findByDraftStatusAndAgreementStatusAndPublishedBeforeAndExpiredAfter(draftStatus: DraftStatus = DraftStatus.DONE,
                                                                                         status: AgreementStatus, published: LocalDateTime? = LocalDateTime.now(),
                                                                                         expired: LocalDateTime? = LocalDateTime.now()): List<AgreementRegistration>

        suspend fun find(): List<AgreementPDTO>

}

@Introspected
data class AgreementPDTO(val title:String,
                         val reference: String,
                         val id: UUID,
                         val published: LocalDateTime,
                         val expired: LocalDateTime)
