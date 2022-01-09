package presentation.log.bloc

import domain.JobLogEntry
import domain.LogLevel
import java.time.LocalDateTime

sealed class JobLogState(
  open val filter: String,
  open val logLevel: LogLevel,
  open val latestRefresh: LocalDateTime?,
  open val logEntries: List<JobLogEntry>,
  open val status: String,
) {
  data class Error(
    override val filter: String,
    override val logLevel: LogLevel,
    override val latestRefresh: LocalDateTime?,
    val errorMessage: String,
  ) : JobLogState(
    filter = filter,
    logLevel = logLevel,
    latestRefresh = latestRefresh,
    logEntries = emptyList(),
    status = errorMessage,
  )

  object Initial : JobLogState(
    filter = "",
    logLevel = LogLevel.Any,
    latestRefresh = null,
    logEntries = emptyList(),
    status = "Idle",
  )

  data class Loading(
    override val filter: String,
    override val logLevel: LogLevel,
    override val latestRefresh: LocalDateTime?,
  ) : JobLogState(
    filter = filter,
    logLevel = logLevel,
    latestRefresh = latestRefresh,
    logEntries = emptyList(),
    status = "Loading...",
  )

  data class Loaded(
    override val filter: String,
    override val logLevel: LogLevel,
    override val latestRefresh: LocalDateTime,
    override val logEntries: List<JobLogEntry>,
  ) : JobLogState(
    filter = filter,
    logLevel = logLevel,
    latestRefresh = latestRefresh,
    logEntries = logEntries,
    status = "Loaded",
  )
}
