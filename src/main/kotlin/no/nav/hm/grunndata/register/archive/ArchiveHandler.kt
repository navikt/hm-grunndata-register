package no.nav.hm.grunndata.register.archive

interface ArchiveHandler {

    fun getArchiveType(): ArchiveType

    suspend fun archive(): List<Archive>

    suspend fun unArchive(unarchive: Archive)

}