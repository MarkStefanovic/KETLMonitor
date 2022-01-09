package adapter

import domain.JobResult
import domain.JobResultRepo
import javax.sql.DataSource

data class PgJobResultRepo(
  val schema: String,
  val showSQL: Boolean,
  private val ds: DataSource,
) : JobResultRepo {
  override suspend fun getLatestResults(): List<JobResult> {
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
      |  ORDER BY 
      |    CASE j.result WHEN 'failed' THEN 0 ELSE 1 END
      |  , j.start_time
    """.trimMargin()

    if (showSQL) {
      println(
        """
        |PgJobResultsRepo.getLatestResult:
        |  ${sql.split("\n").joinToString("\n    ")}
      """.trimMargin()
      )
    }

    ds.connection.use { connection ->
      connection.createStatement().use { statement ->
        val result = statement.executeQuery(sql)

        val jobResults = mutableListOf<JobResult>()
        while (result.next()) {
          val jobName = result.getString("job_name")
          val startTime = result.getTimestamp("start_time").toLocalDateTime()
          val endTime = result.getTimestamp("end_time").toLocalDateTime()
          val resultTypeName = result.getString("result")
          val errorMessage = result.getObject("error_message") as String?
          val skipReason = result.getObject("skip_reason") as String?

          val jobResult = JobResult(
            jobName = jobName,
            result = resultTypeName,
            start = startTime,
            end = endTime,
            skipReason = skipReason,
            errorMessage = errorMessage,
          )

          jobResults.add(jobResult)
        }
        return jobResults
      }
    }
  }
}
