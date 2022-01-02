package domain

import java.time.LocalDateTime

data class JobLogEntry(
  val jobName: String,
  val logLevel: LogLevel,
  val message: String,
  val ts: LocalDateTime,
)
