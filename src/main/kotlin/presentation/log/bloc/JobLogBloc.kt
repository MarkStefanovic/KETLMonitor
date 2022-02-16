package presentation.log.bloc

import domain.JobLogRepo
import domain.LogLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@FlowPreview
@ExperimentalCoroutinesApi
class JobLogBloc(
  private val repo: JobLogRepo,
  private val events: JobLogEvents,
  private val maxEntriesToDisplay: Int,
  private val logger: Logger,
  dispatcher: CoroutineDispatcher,
) {
  private val _state = MutableStateFlow<JobLogState>(JobLogState.Initial)

  val state = _state.asStateFlow()

  private val singleThreadedDispatcher = dispatcher.limitedParallelism(1)

  private var latestRefresh: LocalDateTime? = null

  private var filter = ""

  private var logLevel = LogLevel.Any

  suspend fun autorefreshEvery(duration: Duration) = coroutineScope {
    while (coroutineContext.isActive) {
      events.refresh()

      delay(duration)
    }
  }

  suspend fun start() {
    events.stream.collect { event: JobLogEvent ->
      println("${javaClass.simpleName} received event: $event")

      try {
        withContext(singleThreadedDispatcher) {
          withTimeout(60.seconds) {
            when (event) {
              is JobLogEvent.FilterChanged -> {
                filter = event.prefix

                logLevel = event.logLevel

                _state.emit(
                  JobLogState.Loading(
                    filter = filter,
                    logLevel = logLevel,
                    latestRefresh = latestRefresh,
                  )
                )
              }
              JobLogEvent.Refresh -> {
                _state.emit(
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

            _state.emit(
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
        logger.severe(e.stackTraceToString())
        delay(10.seconds)
      }
    }
  }
}
