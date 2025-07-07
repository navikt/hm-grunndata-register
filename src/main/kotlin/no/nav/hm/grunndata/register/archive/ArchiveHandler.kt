package no.nav.hm.grunndata.register.archive

interface ArchiveHandler<T> {

    fun getArchivePayloadClass(): Class<out T>

    suspend fun archive(toArchive: T): List<Archive>

    suspend fun unArchive(archive: Archive): T

    suspend fun toBeArchived(): List<T>
}