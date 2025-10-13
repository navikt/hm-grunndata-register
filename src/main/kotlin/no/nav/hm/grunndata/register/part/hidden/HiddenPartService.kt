package no.nav.hm.grunndata.register.part.hidden

import jakarta.inject.Singleton
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.toList

@Singleton
class HiddenPartService(private val hiddenPartRepository: HiddenPartRepository) {
    suspend fun hide(productId: UUID, reason: String?, createdBy: String): HiddenPart {
        val existing = hiddenPartRepository.findByProductId(productId)
        return if (existing != null) {
            // update reason if changed
            if (reason != null && reason != existing.reason) {
                hiddenPartRepository.update(existing.copy(reason = reason))
            } else existing
        } else {
            hiddenPartRepository.save(
                HiddenPart(
                    productId = productId,
                    reason = reason,
                    created = LocalDateTime.now(),
                    createdBy = createdBy
                )
            )
        }
    }

    suspend fun unhide(productId: UUID) {
        hiddenPartRepository.findByProductId(productId)?.let { hiddenPartRepository.delete(it) }
    }

    suspend fun isHidden(productId: UUID): Boolean = hiddenPartRepository.existsByProductId(productId)

    suspend fun listHidden(): List<HiddenPart> = hiddenPartRepository.findAll().toList()
}
