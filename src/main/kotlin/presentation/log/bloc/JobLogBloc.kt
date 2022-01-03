package presentation.log.bloc

import domain.JobLogRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlin.time.Duration

@FlowPreview
@ExperimentalCoroutinesApi
class JobLogBloc(
  private val repo: JobLogRepo,
  private val events: JobLogEvents,
  private val maxEntriesToDisplay: Int,
) {
  private val _state = MutableStateFlow<JobLogState>(JobLogState.Initial)

  val state = _state.asStateFlow()

  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private var latestRefresh: LocalDateTime? = null

  private var filter = ""

  suspend fun autorefreshEvery(duration: Duration) = coroutineScope {
    while (coroutineContext.isActive) {
      events.refresh()
      delay(duration)
    }
  }

  suspend fun start() {
    events.stream.collect { event: JobLogEvent ->
      println("JobLogBloc:\n  event: $event")

      withContext(dispatcher) {
        when (event) {
          is JobLogEvent.FilterChanged -> {
            filter = event.prefix

            _state.emit(
              JobLogState.Loading(
                filter = filter,
                latestRefresh = latestRefresh,
              )
            )
          }
          JobLogEvent.Refresh -> {
            _state.emit(
              JobLogState.Loading(
                filter = filter,
                latestRefresh = latestRefresh,
              )
            )
          }
        }
      }

      val logEntries = if (filter.isBlank()) {
        repo.getLatestEntries(n = maxEntriesToDisplay)
      } else {
        repo.getLatestEntriesForJobLike(jobNamePrefix = filter, n = maxEntriesToDisplay)
      }

      withContext(dispatcher) {
        latestRefresh = LocalDateTime.now()

        _state.emit(
          JobLogState.Loaded(
            filter = filter,
            latestRefresh = latestRefresh ?: LocalDateTime.now(),
            logEntries = logEntries,
          )
        )
      }
    }
  }

//  suspend fun start() {
//    events.stream.collect { event: JobLogEvent ->
//      println("JobLogBloc:\n  event: $event")
//
//      withContext(dispatcher) {
//        when (event) {
//          is JobLogEvent.FilterChanged -> {
//            filter = event.prefix
//
//            _state.emit(
//              JobLogState.Loading(
//                filter = filter,
//                latestRefresh = latestRefresh,
//              )
//            )
//          }
//          JobLogEvent.Refresh -> {
//            _state.emit(
//              JobLogState.Loading(
//                filter = filter,
//                latestRefresh = latestRefresh,
//              )
//            )
//          }
//        }
//      }
//
//      val logEntries = if (filter.isBlank()) {
//        repo.getLatestEntries(n = maxEntriesToDisplay)
//      } else {
//        repo.getLatestEntriesForJobLike(jobNamePrefix = filter, n = maxEntriesToDisplay)
//      }
//
//      withContext(dispatcher) {
//        latestRefresh = LocalDateTime.now()
//
//        _state.emit(
//          JobLogState.Loaded(
//            filter = filter,
//            latestRefresh = latestRefresh ?: LocalDateTime.now(),
//            logEntries = logEntries,
//          )
//        )
//      }
//    }
//  }
}
