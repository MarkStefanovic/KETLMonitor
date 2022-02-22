package presentation.result.bloc

import domain.ResultFilter

sealed class JobResultEvent {
  data class FilterChanged(
    val selectedJob: String,
    val jobNamePrefix: String,
    val resultFilter: ResultFilter,
  ) : JobResultEvent()

  object RefreshButtonClicked : JobResultEvent()
}
