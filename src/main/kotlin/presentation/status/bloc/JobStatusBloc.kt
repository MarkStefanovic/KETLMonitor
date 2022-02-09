package presentation.status.bloc

import domain.JobStatus
import domain.JobStatusRepo
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
import kotlin.time.ExperimentalTime

@ExperimentalTime
class JobStatusBloc(
  private val events: JobStatusEvents,
  private val repo: JobStatusRepo,
) {
  private val _state = MutableStateFlow<JobStatusState>(JobStatusState.Initial)

  val state = _state.asStateFlow()

  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private var latestRefresh: LocalDateTime? = null

  private var filter: String = ""

  private val unfilteredJobStatuses = mutableListOf<JobStatus>()

  private val filteredJobStatuses: List<JobStatus>
    get() = unfilteredJobStatuses.filter { it.jobName.lowercase().contains(filter.lowercase()) }

  suspend fun autorefreshEvery(duration: Duration) = coroutineScope {
    while (coroutineContext.isActive) {
      events.refresh()
      delay(duration)
    }
  }

  suspend fun start() {
    events.stream.collect { event: JobStatusEvent ->
      when (event) {
        is JobStatusEvent.Error -> _state.emit(
          JobStatusState.Error(
            latestRefresh = latestRefresh,
            errorMessage = event.errorMessage,
          )
        )
        is JobStatusEvent.FilterChanged -> {
          filter = event.jobName

          val newState = JobStatusState.Loaded(
            filter = filter,
            jobStatuses = filteredJobStatuses,
            latestRefresh = latestRefresh ?: error("Latest refresh is null, but the state is Loaded."),
          )

          _state.emit(newState)
        }
        JobStatusEvent.RefreshButtonClicked -> {
          withContext(dispatcher) {
            _state.emit(
              JobStatusState.Loading(
                jobStatuses = filteredJobStatuses,
                filter = filter,
                latestRefresh = latestRefresh,
              )
            )

            val jobStatuses = repo.getLatestStatuses()

            latestRefresh = LocalDateTime.now()

            unfilteredJobStatuses.clear()
            unfilteredJobStatuses.addAll(jobStatuses)

            val newState = JobStatusState.Loaded(
              filter = filter,
              jobStatuses = filteredJobStatuses,
              latestRefresh = latestRefresh ?: error("Latest refresh is null, but the state is Loaded."),
            )

            _state.emit(newState)
          }
        }
      }
    }
  }
}
