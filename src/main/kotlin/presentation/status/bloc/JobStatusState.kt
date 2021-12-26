package presentation.status.bloc

import domain.JobStatus
import java.time.LocalDateTime

sealed class JobStatusState(
  open val status: String,
  open val latestRefresh: LocalDateTime?,
) {
  object Initial : JobStatusState(
    status = "Idle",
    latestRefresh = null,
  )

  data class Error(
    override val latestRefresh: LocalDateTime?,
    val errorMessage: String,
  ) : JobStatusState(
    status = "Idle",
    latestRefresh = latestRefresh,
  )

  data class Loading(
    val jobStatuses: List<JobStatus>,
    val filter: String,
    override val latestRefresh: LocalDateTime?,
  ) : JobStatusState(
    status = "Loading...",
    latestRefresh = latestRefresh,
  )

  data class Loaded(
    val jobStatuses: List<JobStatus>,
    val filter: String,
    override val latestRefresh: LocalDateTime,
  ) : JobStatusState(
    status = "Idle",
    latestRefresh = latestRefresh,
  )
}
