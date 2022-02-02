package presentation.result.bloc

import domain.JobResult
import domain.ResultFilter
import java.time.LocalDateTime

sealed class JobResultState(
  open val jobNameFilter: String,
  open val selectedRow: Int?,
  open val latestRefresh: LocalDateTime?,
  open val jobResults: List<JobResult>,
  open val status: String,
  open val resultFilter: ResultFilter,
) {
  object Initial : JobResultState(
    jobNameFilter = "",
    selectedRow = null,
    latestRefresh = null,
    jobResults = emptyList(),
    status = "Idle",
    resultFilter = ResultFilter.All,
  )

  data class Error(
    override val jobNameFilter: String,
    override val latestRefresh: LocalDateTime?,
    override val resultFilter: ResultFilter,
    val errorMessage: String,
  ) : JobResultState(
    jobNameFilter = jobNameFilter,
    latestRefresh = latestRefresh,
    selectedRow = null,
    jobResults = emptyList(),
    status = errorMessage,
    resultFilter = resultFilter,
  )

  data class Loading(
    override val jobNameFilter: String,
    override val selectedRow: Int?,
    override val jobResults: List<JobResult>,
    override val latestRefresh: LocalDateTime?,
    override val resultFilter: ResultFilter,
  ) : JobResultState(
    jobNameFilter = jobNameFilter,
    selectedRow = selectedRow,
    latestRefresh = latestRefresh,
    jobResults = jobResults,
    resultFilter = resultFilter,
    status = "Refreshing...",
  )

  data class Loaded(
    override val jobNameFilter: String,
    override val selectedRow: Int?,
    override val jobResults: List<JobResult>,
    override val latestRefresh: LocalDateTime,
    override val resultFilter: ResultFilter,
  ) : JobResultState(
    jobNameFilter = jobNameFilter,
    selectedRow = selectedRow,
    latestRefresh = latestRefresh,
    jobResults = jobResults,
    resultFilter = resultFilter,
    status = "Idle",
  )
}
