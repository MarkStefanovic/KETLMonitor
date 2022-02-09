package presentation.result.bloc

import domain.JobResult
import domain.ResultFilter
import java.time.LocalDateTime

sealed class JobResultState(
  open val jobNameFilter: String,
  open val selectedRow: Int?,
  open val latestRefresh: LocalDateTime?,
  open val jobResults: List<JobResult>,
  open val jobNameOptions: List<String>,
  open val status: String,
  open val resultFilter: ResultFilter,
  open val selectedJob: String,
) {
  object Initial : JobResultState(
    jobNameFilter = "",
    selectedRow = null,
    latestRefresh = null,
    jobResults = emptyList(),
    jobNameOptions = emptyList(),
    status = "Idle",
    resultFilter = ResultFilter.All,
    selectedJob = "All",
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
    jobNameOptions = emptyList(),
    status = errorMessage,
    resultFilter = resultFilter,
    selectedJob = "All",
  )

  data class Loading(
    override val jobNameFilter: String,
    override val selectedRow: Int?,
    override val jobResults: List<JobResult>,
    override val jobNameOptions: List<String>,
    override val latestRefresh: LocalDateTime?,
    override val resultFilter: ResultFilter,
    override val selectedJob: String,
  ) : JobResultState(
    jobNameFilter = jobNameFilter,
    selectedRow = selectedRow,
    latestRefresh = latestRefresh,
    jobResults = jobResults,
    jobNameOptions = jobNameOptions,
    resultFilter = resultFilter,
    status = "Refreshing...",
    selectedJob = selectedJob,
  )

  data class Loaded(
    override val jobNameFilter: String,
    override val selectedRow: Int?,
    override val jobResults: List<JobResult>,
    override val jobNameOptions: List<String>,
    override val latestRefresh: LocalDateTime,
    override val resultFilter: ResultFilter,
    override val selectedJob: String,
  ) : JobResultState(
    jobNameFilter = jobNameFilter,
    selectedRow = selectedRow,
    latestRefresh = latestRefresh,
    jobResults = jobResults,
    jobNameOptions = jobNameOptions,
    resultFilter = resultFilter,
    status = "Idle",
    selectedJob = selectedJob,
  )
}
