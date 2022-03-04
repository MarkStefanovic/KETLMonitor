package presentation.log.bloc

import domain.JobLogRepo
import domain.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.util.logging.Logger
import kotlin.time.Duration

@FlowPreview
@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
fun CoroutineScope.jobLogBloc(
  repo: JobLogRepo,
  events: JobLogEvents,
  states: JobLogStates,
  maxEntriesToDisplay: Int,
  logger: Logger,
) {
  val mutex = Mutex()

  var latestRefresh: LocalDateTime? = null

  var filter = ""

  var logLevel = LogLevel.Any

  launch {
    events.stream.collect { event: JobLogEvent ->
      logger.info("jobResultBloc received event: $event")

      try {
        when (event) {
          is JobLogEvent.FilterChanged -> {
            mutex.withLock {
              filter = event.prefix
              logLevel = event.logLevel
            }

            states.emit(
              JobLogState.Loading(
                filter = filter,
                logLevel = logLevel,
                latestRefresh = latestRefresh,
              )
            )
          }
          JobLogEvent.Refresh -> {
            states.emit(
              JobLogState.Loading(
                filter = filter,
                logLevel = logLevel,
                latestRefresh = latestRefresh,
              )
            )
          }
        }

        val logEntries = repo.where(
          jobNamePrefix = filter,
          logLevel = logLevel,
          n = maxEntriesToDisplay,
        )

        mutex.withLock {
          latestRefresh = LocalDateTime.now()
        }

        states.emit(
          JobLogState.Loaded(
            filter = filter,
            logLevel = logLevel,
            latestRefresh = latestRefresh ?: LocalDateTime.now(),
            logEntries = logEntries,
          )
        )
      } catch (e: Exception) {
        logger.severe(e.stackTraceToString())
        throw e
      }
    }
  }
}

fun CoroutineScope.refreshJobLogEvery(
  events: JobLogEvents,
  duration: Duration,
) = launch {
  while (isActive) {
    events.refresh()
    delay(duration)
  }
}
