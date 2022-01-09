package domain

interface JobLogRepo {
  fun where(jobNamePrefix: String, logLevel: LogLevel, n: Int): List<JobLogEntry>
}
