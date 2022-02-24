package presentation.log.bloc

import domain.JobLogRepo
import domain.LogLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
  val singleThreadedContext = newSingleThreadContext("jobResultBloc")

  var latestRefresh: LocalDateTime? = null

  var filter = ""

  var logLevel = LogLevel.Any

  launch {
    events.stream.collect { event: JobLogEvent ->
      logger.info("jobResultBloc received event: $event")

      try {
        withContext(singleThreadedContext) {
          withTimeout(20.seconds) {
            when (event) {
              is JobLogEvent.FilterChanged -> {
                filter = event.prefix

                logLevel = event.logLevel

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

            latestRefresh = LocalDateTime.now()

            states.emit(
              JobLogState.Loaded(
                filter = filter,
                logLevel = logLevel,
                latestRefresh = latestRefresh ?: LocalDateTime.now(),
                logEntries = logEntries,
              )
            )
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) {
          if (e is TimeoutCancellationException) {
            logger.info("jobLogBloc timed out.")
          } else {
            logger.info("Cancelling jobLogBloc...")
            throw e
          }
        }

        logger.severe(e.stackTraceToString())
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
