package presentation.result.bloc

import domain.JobResult
import java.time.LocalDateTime

sealed class JobResultState(
  open val status: String,
  open val latestRefresh: LocalDateTime?,
) {
  object Initial : JobResultState(
    latestRefresh = null,
    status = "Idle",
  )

  data class Error(
    override val latestRefresh: LocalDateTime?,
    val errorMessage: String,
  ) : JobResultState(
    latestRefresh = latestRefresh,
    status = errorMessage,
  )

  data class Loading(
    override val latestRefresh: LocalDateTime?,
  ) : JobResultState(
    latestRefresh = latestRefresh,
    status = "Refreshing...",
  )

  data class Loaded(
    val jobResults: List<JobResult>,
    val jobNameOptions: List<String>,
    override val latestRefresh: LocalDateTime,
  ) : JobResultState(
    latestRefresh = latestRefresh,
    status = "Idle",
  )
}
