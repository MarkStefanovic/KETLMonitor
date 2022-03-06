@file:Suppress("SqlResolve", "SqlNoDataSourceInspection")

package adapter

import domain.JobResult
import domain.JobResultRepo
import domain.ResultFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.sql.ResultSet
import javax.sql.DataSource

@ExperimentalCoroutinesApi
class PgJobResultRepo(
  val schema: String,
  val showSQL: Boolean,
  private val ds: DataSource,
  dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JobResultRepo {
  private val pool = dispatcher.limitedParallelism(1)

  override suspend fun getLatestResults(
    jobNameStartsWith: String?,
    resultFilter: ResultFilter,
  ): List<JobResult> = withContext(pool) {
    if (resultFilter == ResultFilter.All) {
      // language=PostgreSQL
      val sql = """
      |  SELECT 
      |    j.job_name
      |  , j.start_time
      |  , j.end_time
      |  , j.result
      |  , j.error_message
      |  , j.skip_reason
      |  FROM $schema.job_result_snapshot AS j
      |  WHERE 
      |    STARTS_WITH(j.job_name, ?)
      |  ORDER BY 
      |    j.end_time DESC
      """.trimMargin()

      if (showSQL) {
        println(
          """
        |${javaClass.simpleName}.getLatestResult(jobNameStartsWith = $jobNameStartsWith, resultFilter = $resultFilter):
        |  ${sql.split("\n").joinToString("\n    ")}
          """.trimMargin()
        )
      }

      ds.connection.use { con ->
        con.prepareStatement(sql).use { preparedStatement ->
          preparedStatement.setString(1, jobNameStartsWith)

          val result = preparedStatement.executeQuery()

          val results = mutableListOf<JobResult>()
          while (result.next()) {
            results.add(result.toDomain())
          }
          results
        }
      }
    } else {
      // language=PostgreSQL
      val sql = """
      |  SELECT 
      |    j.job_name
      |  , j.start_time
      |  , j.end_time
      |  , j.result
      |  , j.error_message
      |  , j.skip_reason
      |  FROM $schema.job_result_snapshot AS j
      |  WHERE 
      |    j.result = ?
      |    AND STARTS_WITH(j.job_name, ?)
      |  ORDER BY 
      |    j.end_time DESC
      """.trimMargin()

      if (showSQL) {
        println(
          """
        |${javaClass.simpleName}.getLatestResult(jobNameStartsWith = $jobNameStartsWith, resultFilter = $resultFilter):
        |  ${sql.split("\n").joinToString("\n    ")}
          """.trimMargin()
        )
      }

      ds.connection.use { con ->
        con.prepareStatement(sql).use { preparedStatement ->
          preparedStatement.setString(1, jobNameStartsWith)
          preparedStatement.setString(2, resultFilter.dbName)

          val result = preparedStatement.executeQuery()

          val results = mutableListOf<JobResult>()
          while (result.next()) {
            results.add(result.toDomain())
          }
          results
        }
      }
    }
  }

  override suspend fun getResultsForJob(
    selectedJob: String,
    resultFilter: ResultFilter,
  ): List<JobResult> = withContext(pool) {
    if (resultFilter == ResultFilter.All) {
      // language=PostgreSQL
      val sql = """
      |  SELECT 
      |    j.job_name
      |  , j.start_time
      |  , j.end_time
      |  , j.result
      |  , j.error_message
      |  , j.skip_reason
      |  FROM $schema.job_result AS j
      |  WHERE 
      |    j.job_name = ?
      |  ORDER BY 
      |    j.end_time DESC
      |  LIMIT 1000
      """.trimMargin()

      if (showSQL) {
        println(
          """
          |${javaClass.simpleName}.getLatestResult(selectedJob = $selectedJob, resultFilter = $resultFilter):
          |  ${sql.split("\n").joinToString("\n    ")}
          """.trimMargin()
        )
      }

      ds.connection.use { con ->
        con.prepareStatement(sql).use { statement ->
          statement.setString(1, selectedJob)

          val result = statement.executeQuery()

          val results = mutableListOf<JobResult>()
          while (result.next()) {
            results.add(result.toDomain())
          }
          results
        }
      }
    } else {
      // language=PostgreSQL
      val sql = """
      |  SELECT
      |    j.job_name
      |  , j.start_time
      |  , j.end_time
      |  , j.result
      |  , j.error_message
      |  , j.skip_reason
      |  FROM $schema.job_result AS j
      |  WHERE
      |    j.job_name = ?
      |    AND j.result = ?
      |  ORDER BY
      |    j.end_time DESC
      |  LIMIT 1000
      """.trimMargin()

      if (showSQL) {
        println(
          """
          |${javaClass.simpleName}.getLatestResult(selectedJob = $selectedJob, resultFilter = $resultFilter):
          |  ${sql.split("\n").joinToString("\n    ")}
          """.trimMargin()
        )
      }

      ds.connection.use { con ->
        con.prepareStatement(sql).use { statement ->
          statement.setString(1, selectedJob)
          statement.setString(2, resultFilter.dbName)

          val result = statement.executeQuery()

          val results = mutableListOf<JobResult>()
          while (result.next()) {
            results.add(result.toDomain())
          }
          results
        }
      }
    }
  }
}

fun ResultSet.toDomain(): JobResult =
  JobResult(
    jobName = this.getString("job_name"),
    start = this.getTimestamp("start_time").toLocalDateTime(),
    end = this.getTimestamp("end_time").toLocalDateTime(),
    result = this.getString("result"),
    errorMessage = this.getObject("error_message") as String?,
    skipReason = this.getObject("skip_reason") as String?,
  )
