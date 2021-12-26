package presentation.result.bloc

import domain.JobResult
import java.time.LocalDateTime

sealed class JobResultState(
  open val jobNameFilter: String,
  open val selectedRow: Int?,
  open val latestRefresh: LocalDateTime?,
  open val jobResults: List<JobResult>,
  open val status: String,
) {
  object Initial : JobResultState(
    jobNameFilter = "",
    selectedRow = null,
    latestRefresh = null,
    jobResults = emptyList(),
    status = "Idle",
  )

  data class Error(
    override val jobNameFilter: String,
    override val latestRefresh: LocalDateTime?,
    val errorMessage: String,
  ) : JobResultState(
    jobNameFilter = jobNameFilter,
    latestRefresh = latestRefresh,
    selectedRow = null,
    jobResults = emptyList(),
    status = errorMessage,
  )

  data class Loading(
    override val jobNameFilter: String,
    override val selectedRow: Int?,
    override val jobResults: List<JobResult>,
    override val latestRefresh: LocalDateTime?,
  ) : JobResultState(
    jobNameFilter = jobNameFilter,
    selectedRow = selectedRow,
    latestRefresh = latestRefresh,
    jobResults = jobResults,
    status = "Refreshing...",
  )

  data class Loaded(
    override val jobNameFilter: String,
    override val selectedRow: Int?,
    override val jobResults: List<JobResult>,
    override val latestRefresh: LocalDateTime,
  ) : JobResultState(
    jobNameFilter = jobNameFilter,
    selectedRow = selectedRow,
    latestRefresh = latestRefresh,
    jobResults = jobResults,
    status = "Idle",
  )
}
