package presentation.log.bloc

import domain.LogLevel

sealed class JobLogEvent {
  object Refresh : JobLogEvent()

  data class FilterChanged(val prefix: String, val logLevel: LogLevel) : JobLogEvent()
}
