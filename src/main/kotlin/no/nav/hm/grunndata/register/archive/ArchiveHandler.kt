package no.nav.hm.grunndata.register.archive

interface ArchiveHandler<T> {

    fun getArchivePayloadClass(): Class<out T>

    suspend fun archive(): List<Archive>

    suspend fun unArchive(archive: Archive): T

}