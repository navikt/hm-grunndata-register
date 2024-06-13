package no.nav.hm.grunndata.register.series

import io.micronaut.context.annotation.Factory
import io.micronaut.data.event.listeners.PostPersistEventListener
import io.micronaut.data.event.listeners.PostUpdateEventListener
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

@Factory
class SeriesPersistListener(private val seriesRegistrationVersionService: SeriesRegistrationVersionService) {

    companion object {
        private val LOG = LoggerFactory.getLogger(SeriesPersistListener::class.java)
    }

    @Singleton
    fun afterSeriesRegistrationVersionPersist(): PostPersistEventListener<SeriesRegistration> {
        return PostPersistEventListener { series: SeriesRegistration ->
            runBlocking {
                LOG.debug("SeriesRegistrationVersion inserted for series: {}", series.id)
                insertSeriesVersion(series)
            }
        }
    }

    @Singleton
    fun afterSeriesRegistrationVersionUpdate(): PostUpdateEventListener<SeriesRegistration> {
        return PostUpdateEventListener { series: SeriesRegistration ->
            runBlocking {
                LOG.debug("SeriesRegistrationVersion updated for series: {}", series.id)
                insertSeriesVersion(series)
            }
        }
    }

    private suspend fun insertSeriesVersion(series: SeriesRegistration) {
        seriesRegistrationVersionService.save(series.toVersion())
    }

    private fun SeriesRegistration.toVersion(): SeriesRegistrationVersion = SeriesRegistrationVersion(
            seriesId = this.id,
            version = this.version,
            draftStatus = this.draftStatus,
            adminStatus = this.adminStatus,
            status = this.status,
            updated = this.updated,
            seriesRegistration = this
        )
}