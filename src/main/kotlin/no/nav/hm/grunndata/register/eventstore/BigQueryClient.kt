package no.nav.hm.grunndata.register.eventstore

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.DatasetId
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory


interface BigQueryClient {
    /**
     * Sjekk om datasett finnes
     */
    fun datasetPresent(datasetId: DatasetId): Boolean

    /**
     * Sjekk om tabell finnes
     */
    fun tablePresent(tableId: TableId): Boolean

    /**
     * Opprett tabell i BigQuery
     */
    fun create(tableInfo: TableInfo): TableInfo

    /**
     * Oppdater tabell i BigQuery
     */
    fun update(tableId: TableId, updatedTableInfo: TableInfo): Boolean

    /**
     * Sett in rad i BigQuery-tabell
     */
    fun insert(tableId: TableId, row: RowToInsert)

    class BigQueryClientException(message: String) : RuntimeException(message)
}

@Singleton
class DefaultBigQueryClient(@Value("\${gcp.team.project.id}") private val projectId: String) : BigQueryClient {
    private val bigQuery = BigQueryOptions.newBuilder()
        .setProjectId(projectId)
        .build()
        .service

    companion object {
        private val LOG = LoggerFactory.getLogger(DefaultBigQueryClient::class.java)
    }

    private fun getTable(tableId: TableId): Table = checkNotNull(bigQuery.getTable(tableId)) {
        "Mangler tabell: '${tableId.table}' i BigQuery"
    }

    override fun datasetPresent(datasetId: DatasetId): Boolean =  bigQuery.getDataset(datasetId) != null

    override fun tablePresent(tableId: TableId): Boolean = bigQuery.getTable(tableId) != null

    override fun create(tableInfo: TableInfo): TableInfo = bigQuery.create(tableInfo)

    override fun update(
        tableId: TableId,
        updatedTableInfo: TableInfo,
    ): Boolean {
        val table = getTable(tableId)
        return when (TableInfo.of(tableId, table.getDefinition())) {
            updatedTableInfo -> {
                LOG.info ("Skjema for tabell: ${tableId.table} er uendret, oppdaterer ikke tabell i BigQuery" )
                false
            }

            else -> {
                LOG.info("Skjema for tabell: ${tableId.table} er endret, oppdaterer tabell i BigQuery" )
                val updatedTable = table.toBuilder()
                    .setDescription(updatedTableInfo.description)
                    .setDefinition(updatedTableInfo.getDefinition())
                    .build()
                updatedTable.update()
                true
            }
        }
    }

    override fun insert(tableId: TableId, row: RowToInsert)  {
        val table = getTable(tableId)
        val rows = listOf(row)
        LOG.debug (
            "Setter inn rader i tabell: '${tableId.table}', rader: '$rows'"
        )
        val response = table.insert(rows)
        when {
            response.hasErrors() -> throw BigQueryClient.BigQueryClientException(
                "Lagring i BigQuery feilet: '${response.insertErrors}'"
            )

            else -> LOG.debug ("Rader ble lagret i tabell: '${tableId.table}'" )
        }
    }
}

class LocalBigQueryClient : BigQueryClient {
    companion object {
        private val LOG = LoggerFactory.getLogger(LocalBigQueryClient::class.java)
    }

    override fun datasetPresent(datasetId: DatasetId): Boolean {
        LOG.info ("datasetPresent(datasetId) called with datasetId: '$datasetId'" )
        return true
    }

    override fun tablePresent(tableId: TableId): Boolean {
        LOG.info ( "tablePresent(tableId) called with tableId: '$tableId'" )
        return true
    }

    override fun create(tableInfo: TableInfo): TableInfo {
        LOG.info ("create(tableInfo) called with tableInfo: '$tableInfo'" )
        return tableInfo
    }

    override fun update(tableId: TableId, updatedTableInfo: TableInfo): Boolean {
        LOG.info("update(tableId, updatedTableInfo) called with tableId: '$tableId', updatedTableInfo: $updatedTableInfo" )
        return true
    }

    override fun insert(tableId: TableId, row: RowToInsert) {
        LOG.info ("insert(tableId, row) called with tableId: '$tableId', row: '$row'" )
    }

}