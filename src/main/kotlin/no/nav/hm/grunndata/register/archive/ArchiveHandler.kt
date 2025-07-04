package no.nav.hm.grunndata.register.archive

interface ArchiveHandler<T> {

    fun getArchivePayloadClass(): Class<out T>

    fun toArchive(toArchive: T): Archive

    fun unArchive(archive: Archive): T

}