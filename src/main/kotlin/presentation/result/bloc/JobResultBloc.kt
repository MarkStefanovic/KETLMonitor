package presentation.result.bloc

import domain.JobResult
import domain.JobResultRepo
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
class JobResultBloc(
  private val repo: JobResultRepo,
  private val events: JobResultEvents,
) {
  private var _state = MutableStateFlow<JobResultState>(JobResultState.Initial)

  val state = _state.asStateFlow()

  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private var latestRefresh: LocalDateTime? = null

  private var filter = ""

  private var selectedRow: Int? = null

  private val unfilteredJobResults = mutableListOf<JobResult>()

  private val filteredJobResults: List<JobResult>
    get() = unfilteredJobResults.filter { it.jobName.lowercase().contains(filter.lowercase()) }

  suspend fun autorefreshEvery(duration: Duration) = coroutineScope {
    while (coroutineContext.isActive) {
      events.refresh()
      delay(duration)
    }
  }

  suspend fun start() {
    events.stream.collect { event ->
      println("JobResultDashBloc received refresh event.")

      when (event) {
        is JobResultEvent.RowSelected -> {
          when (val st = state.value) {
            is JobResultState.Error -> throw Exception("Row was selected, but state was not Loaded.")
            JobResultState.Initial -> throw Exception("Row was selected, but state was not Loaded.")
            is JobResultState.Loaded -> {
              withContext(dispatcher) {
                val newState = JobResultState.Loaded(
                  jobNameFilter = filter,
                  jobResults = filteredJobResults,
                  selectedRow = state.value.selectedRow,
                  latestRefresh = st.latestRefresh,
                )

                _state.emit(newState)
              }
            }
            is JobResultState.Loading -> TODO()
          }
        }
        is JobResultEvent.RefreshButtonClicked -> {
          withContext(dispatcher) {
            val newState = JobResultState.Loading(
              jobNameFilter = state.value.jobNameFilter,
              jobResults = filteredJobResults,
              selectedRow = selectedRow,
              latestRefresh = latestRefresh,
            )
            _state.emit(newState)
          }

          val newJobResults = repo.getLatestResults()

          val lr = LocalDateTime.now()

          withContext(dispatcher) {
            latestRefresh = lr
            unfilteredJobResults.clear()
            unfilteredJobResults.addAll(newJobResults)

            val newState = JobResultState.Loaded(
              jobNameFilter = state.value.jobNameFilter,
              jobResults = filteredJobResults,
              selectedRow = selectedRow,
              latestRefresh = lr,
            )

            println("Emitting new job results after refresh.")

            _state.emit(newState)
          }
        }
        is JobResultEvent.FilterChanged -> {
          println("jobNameFilter changed to '${event.jobNamePrefix}'")

          withContext(dispatcher) {
            filter = event.jobNamePrefix

            val newState = JobResultState.Loaded(
              jobNameFilter = filter,
              jobResults = filteredJobResults,
              selectedRow = selectedRow,
              latestRefresh = latestRefresh ?: error("FilterChanged event triggered, but state was not loaded."),
            )

            _state.emit(newState)
          }
        }
      }
    }
  }
}
