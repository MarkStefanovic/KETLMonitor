package domain

import java.time.LocalDateTime

data class JobStatus(
  val jobName: String,
  val status: String,
  val errorMessage: String?,
  val skipReason: String?,
  val ts: LocalDateTime,
) {
  override fun toString(): String =
    """
      |JobStatus [
      |  jobName: $jobName
      |  status: $status
      |  errorMessage: $errorMessage
      |  skipReason: $skipReason
      |  ts: $ts
      |]
    """.trimMargin()
}
