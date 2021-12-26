package presentation.result.bloc

sealed class JobResultEvent {
  data class FilterChanged(val jobNamePrefix: String) : JobResultEvent()

  object RefreshButtonClicked : JobResultEvent()

  data class RowSelected(val rowNumber: Int) : JobResultEvent()
}
