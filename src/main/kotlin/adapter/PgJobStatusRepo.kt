package adapter

import domain.JobStatus
import domain.JobStatusRepo
import javax.sql.DataSource

data class PgJobStatusRepo(
  val schema: String,
  val showSQL: Boolean,
  private val ds: DataSource,
) : JobStatusRepo {
  override suspend fun getLatestStatuses(): List<JobStatus> {
    // language=PostgreSQL
    val sql = """
    |SELECT 
    |  j.job_name
    |, j.status
    |, j.error_message
    |, j.skip_reason
    |, j.ts
    |FROM $schema.job_status_snapshot AS j
    |ORDER BY 
    |  CASE j.status
    |    WHEN 'running' THEN 0
    |    WHEN 'failed' THEN 1
    |    WHEN 'successful' THEN 2
    |    WHEN 'skipped' THEN 3
    |    ELSE 4
    |  END
    |, j.ts
    """.trimMargin()

    if (showSQL) {
      println(
        """
        |PgJobResultsRepo.getLatestResult:
        |  ${sql.split("\n").joinToString("\n  ")}
      """.trimMargin()
      )
    }

    ds.connection.use { connection ->
      connection.createStatement().use { statement ->
        val result = statement.executeQuery(sql.trimMargin())

        val jobStatuses = mutableListOf<JobStatus>()
        while (result.next()) {
          val jobName = result.getString("job_name")
          val status = result.getString("status")
          val errorMessage = result.getObject("error_message") as String?
          val skipReason = result.getObject("skip_reason") as String?
          val ts = result.getTimestamp("ts").toLocalDateTime()

          val jobStatus = JobStatus(
            jobName = jobName,
            status = status,
            skipReason = skipReason,
            errorMessage = errorMessage,
            ts = ts,
          )

          jobStatuses.add(jobStatus)
        }
        println(jobStatuses)
        return jobStatuses
      }
    }
  }
}
