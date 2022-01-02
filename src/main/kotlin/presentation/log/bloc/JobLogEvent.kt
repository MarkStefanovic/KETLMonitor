package presentation.log.bloc

sealed class JobLogEvent {
  object Refresh : JobLogEvent()

  data class FilterChanged(val prefix: String) : JobLogEvent()
}
