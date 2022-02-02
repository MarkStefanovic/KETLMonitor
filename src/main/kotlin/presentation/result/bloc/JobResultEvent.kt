package presentation.result.bloc

import domain.ResultFilter

sealed class JobResultEvent {
  data class FilterChanged(
    val jobNamePrefix: String,
    val resultFilter: ResultFilter,
  ) : JobResultEvent()

  object RefreshButtonClicked : JobResultEvent()

  data class RowSelected(val rowNumber: Int) : JobResultEvent()
}
