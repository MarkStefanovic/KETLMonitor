package domain

import java.time.Duration
import java.time.LocalDateTime

data class JobResult(
  val jobName: String,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val result: String,
  val skipReason: String?,
  val errorMessage: String?,
) {
  val duration: Duration by lazy {
    Duration.between(start, end)
  }

  override fun toString(): String =
    """
      |JobResult [
      |  jobName: $jobName
      |  start: $start
      |  end: $end
      |  result: $result
      |  skipReason: $skipReason
      |  errorMessage: $errorMessage
      |]
    """.trimMargin()
}
