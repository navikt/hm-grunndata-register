package no.nav.hm.grunndata.register.eventstore

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.DatasetId
import com.google.cloud.bigquery.TableId
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory


@Singleton
class BigQueryService(
    private val bigQueryClient: BigQueryClient,
    @Value("\${bq.dataset}") private val dataSet: String,
    private val objectMapper: ObjectMapper
) {



    companion object {
        private val LOG = LoggerFactory.getLogger(BigQueryService::class.java)
    }

    init {
        val datasetId = DatasetId.of(dataSet)
        LOG.info("Dataset ${dataSet} exists: ${bigQueryClient.datasetPresent(datasetId)}")
    }

    fun checkDataSet() : Boolean {
        val datasetId = DatasetId.of(dataSet)
        return bigQueryClient.datasetPresent(datasetId)
    }

    fun checkTable(tableName: String) : Boolean {
        val tableId = TableId.of(dataSet, tableName)
        return bigQueryClient.tablePresent(tableId)
    }

}

data class BigQueryResponse(val hasError: Boolean, val rowsError: Int)