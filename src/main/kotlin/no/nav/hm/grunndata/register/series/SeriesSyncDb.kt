package no.nav.hm.grunndata.register.series

import jakarta.inject.Singleton
import no.nav.hm.grunndata.rapid.dto.DraftStatus
import no.nav.hm.grunndata.register.gdb.GdbApiClient
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Singleton
class SeriesSyncDb(
    private val gdbApiClient: GdbApiClient,
    private val seriesRegistrationService: SeriesRegistrationService
) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesSyncDb::class.java)
    }

    suspend fun syncSeries() {
        val dateString = LocalDateTime.now().minusYears(30).toString()
        var page = gdbApiClient.fetchSeries(
            params = mapOf("updated" to dateString),
            size = 1000,
            page = 0,
            sort = "updated,asc"
        )
        while (page.pageNumber < page.totalPages) {
            if (page.numberOfElements > 0) {
                val series = page.content
                LOG.info("Got ${series.size} series from grunndata-db")
                series.forEach {
                    seriesRegistrationService.findById(it.id)?.let { inDb ->
                        LOG.info("Updating series ${it.id} from grunndata-db")
                        seriesRegistrationService.update(
                            inDb.copy(
                                identifier = it.id.toString(),
                                title = it.title,
                                status = it.status,
                                updatedBy = it.updatedBy,
                                updated = LocalDateTime.now(),
                                expired = it.expired
                            )
                        )
                    } ?: run {
                        LOG.info("saving new series ${it.id} from grunndata-db")
                        seriesRegistrationService.save(
                            SeriesRegistrationDTO(
                                id = it.id,
                                supplierId = it.supplierId,
                                identifier = it.id.toString(),
                                title = it.title,
                                draftStatus = DraftStatus.DONE,
                                status = it.status,
                                createdBy = it.createdBy,
                                updatedBy = it.updatedBy,
                                createdByUser = "HMDB",
                                updatedByUser = "HMDB",
                                createdByAdmin = false
                            )
                        )
                    }
                }
            }
            page = gdbApiClient.fetchSeries(
                params = mapOf("updated" to dateString),
                size = 1000, page = page.pageNumber + 1, sort = "updated,asc"
            )
        }
    }
}
