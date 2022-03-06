@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package adapter

import domain.JobLogEntry
import domain.JobLogRepo
import domain.LogLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.sql.ResultSet
import javax.sql.DataSource

@ExperimentalCoroutinesApi
class PgJobLogRepo(
  val schema: String,
  val showSQL: Boolean,
  private val ds: DataSource,
  dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JobLogRepo {
  private val pool = dispatcher.limitedParallelism(1)

  override suspend fun where(
    jobNamePrefix: String,
    logLevel: LogLevel,
    n: Int,
  ): List<JobLogEntry> = withContext(pool) {
    val logLevelFilter = when (logLevel) {
      LogLevel.Any -> ""
      LogLevel.Debug -> "AND log_level = 'debug'"
      LogLevel.Error -> "AND log_level = 'error'"
      LogLevel.Info -> "AND log_level = 'info'"
      LogLevel.Warning -> "AND log_level = 'warning'"
    }

    //language=PostgreSQL
    val sql = """
      |SELECT
      |  log_name
      |, log_level
      |, message
      |, ts
      |FROM $schema.log
      |WHERE 
      |  STARTS_WITH(log_name, ?)
      |  $logLevelFilter
      |ORDER BY 
      |  ts DESC
      |LIMIT $n
    """.trimMargin()

    if (showSQL) {
      println(
        """
        |PgJobLogRepo.getLatestEntriesForJobLike(jobNamePrefix = $jobNamePrefix, logLevel = $logLevel, n = $n)
        |  ${sql.split("\n").joinToString("\n  ")}
        """.trimMargin()
      )
    }

    val logEntries = mutableListOf<JobLogEntry>()
    ds.connection.use { connection ->
      connection.prepareStatement(sql).use { preparedStatement ->
        preparedStatement.setString(1, jobNamePrefix)

        preparedStatement.executeQuery().use { resultSet ->
          while (resultSet.next()) {
            val logEntry = resultSet.toJobLogEntry()
            logEntries.add(logEntry)
          }
        }
      }
    }
    logEntries
  }
}

private fun ResultSet.toJobLogEntry(): JobLogEntry =
  JobLogEntry(
    jobName = getString("log_name"),
    logLevel = LogLevel.fromString(getString("log_level")),
    message = getString("message"),
    ts = getTimestamp("ts").toLocalDateTime(),
  )
