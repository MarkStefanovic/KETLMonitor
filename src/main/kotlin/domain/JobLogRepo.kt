package domain

interface JobLogRepo {
  suspend fun where(
    jobNamePrefix: String,
    logLevel: LogLevel,
    n: Int,
  ): List<JobLogEntry>
}
