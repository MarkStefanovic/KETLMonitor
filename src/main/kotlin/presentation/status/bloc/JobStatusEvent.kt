package presentation.status.bloc

sealed class JobStatusEvent {
  data class Error(val errorMessage: String) : JobStatusEvent()

  data class FilterChanged(val jobName: String) : JobStatusEvent()

  object RefreshButtonClicked : JobStatusEvent()
}
