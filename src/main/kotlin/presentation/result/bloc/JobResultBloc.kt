package presentation.result.bloc

import domain.JobResult
import domain.JobResultRepo
import domain.ResultFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class JobResultBloc(
  private val repo: JobResultRepo,
  private val events: JobResultEvents,
  private val logger: Logger,
  dispatcher: CoroutineDispatcher,
) {
  private var _state = MutableStateFlow<JobResultState>(JobResultState.Initial)

  val state = _state.asStateFlow()

  private val singleThreadedDispatcher = dispatcher.limitedParallelism(1)

  private var latestRefresh: LocalDateTime? = null

  private var jobNameFilter = ""

  private var resultFilter = ResultFilter.All

  private var selectedRow: Int? = null

  private var selectedJob: String = "All"

  suspend fun autorefreshEvery(duration: Duration) = coroutineScope {
    while (isActive) {
      events.refresh()

      delay(duration)
    }
  }

  suspend fun start() {
    events.stream.collect { event ->
      try {
        withContext(singleThreadedDispatcher) {
          println("${javaClass.simpleName} received event: $event")

          withTimeout(60.seconds) {
            when (event) {
              is JobResultEvent.RowSelected -> {
                when (val st = state.value) {
                  is JobResultState.Error -> throw Exception("Row was selected, but state was not Loaded.")
                  JobResultState.Initial -> throw Exception("Row was selected, but state was not Loaded.")
                  is JobResultState.Loaded -> {
                    val newState = JobResultState.Loaded(
                      jobNameFilter = jobNameFilter,
                      resultFilter = resultFilter,
                      jobResults = st.jobResults,
                      jobNameOptions = jobNameOptions(st.jobResults),
                      selectedRow = state.value.selectedRow,
                      latestRefresh = st.latestRefresh,
                      selectedJob = selectedJob,
                    )

                    _state.emit(newState)
                  }
                  is JobResultState.Loading -> TODO()
                }
              }
              is JobResultEvent.RefreshButtonClicked -> {
                val loadingState = JobResultState.Loading(
                  jobNameFilter = jobNameFilter,
                  resultFilter = resultFilter,
                  jobResults = state.value.jobResults,
                  jobNameOptions = jobNameOptions(state.value.jobResults),
                  selectedRow = selectedRow,
                  selectedJob = selectedJob,
                  latestRefresh = latestRefresh,
                )
                _state.emit(loadingState)

                val newJobResults = if (selectedJob == "All") {
                  repo.getLatestResults(
                    jobNameStartsWith = jobNameFilter,
                    resultFilter = resultFilter,
                  )
                } else {
                  repo.getResultsForJob(
                    selectedJob = selectedJob,
                    resultFilter = resultFilter,
                  )
                }

                val lr = LocalDateTime.now()

                latestRefresh = lr
                //            unfilteredJobResults.clear()
                //            unfilteredJobResults.addAll(newJobResults)

                val loadedState = JobResultState.Loaded(
                  jobNameFilter = jobNameFilter,
                  resultFilter = resultFilter,
                  jobResults = newJobResults,
                  jobNameOptions = jobNameOptions(newJobResults),
                  selectedJob = selectedJob,
                  selectedRow = selectedRow,
                  latestRefresh = lr,
                )

                println("Emitting new job results after refresh.")

                _state.emit(loadedState)
              }
              is JobResultEvent.FilterChanged -> {
                println(
                  "jobNameFilter changed to '${event.jobNamePrefix}', selectedJob changed to '${event.selectedJob}', " +
                    "and resultFilter changed to $resultFilter"
                )

                jobNameFilter = event.jobNamePrefix
                resultFilter = event.resultFilter
                selectedJob = event.selectedJob

                val newJobResults = if (selectedJob == "All") {
                  repo.getLatestResults(
                    jobNameStartsWith = jobNameFilter,
                    resultFilter = resultFilter,
                  )
                } else {
                  repo.getResultsForJob(
                    selectedJob = selectedJob,
                    resultFilter = resultFilter,
                  )
                }

                val newState = JobResultState.Loaded(
                  jobNameFilter = jobNameFilter,
                  resultFilter = resultFilter,
                  jobResults = newJobResults,
                  jobNameOptions = jobNameOptions(newJobResults),
                  selectedJob = selectedJob,
                  selectedRow = selectedRow,
                  latestRefresh = latestRefresh ?: error("FilterChanged event triggered, but state was not loaded."),
                )

                _state.emit(newState)
              }
            }
          }
        }
      } catch (ce: CancellationException) {
        println("${javaClass.simpleName} closed.")
        throw ce
      } catch (e: Throwable) {
        logger.severe(e.stackTraceToString())
        delay(10.seconds)
      }
    }
  }
}

fun jobNameOptions(jobResults: List<JobResult>) =
  listOf("All") + jobResults.map { it.jobName }.sorted()
