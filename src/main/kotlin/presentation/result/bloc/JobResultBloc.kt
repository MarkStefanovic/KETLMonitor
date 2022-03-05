package presentation.result.bloc

import domain.JobResult
import domain.JobResultRepo
import domain.ResultFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.time.LocalDateTime
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
fun CoroutineScope.jobResultBloc(
  repo: JobResultRepo,
  events: JobResultEvents,
  states: JobResultStates,
  logger: Logger,
) {
  val mutex = Mutex()

  var latestRefresh: LocalDateTime? = null

  var selectedJob: String = "All"
  var jobNamePrefix: String = ""
  var resultFilter: ResultFilter = ResultFilter.All

  launch {
    events.stream.collect { event ->
      logger.info("jobResultBloc received event: $event")

      when (event) {
        is JobResultEvent.RefreshButtonClicked -> {
          val loadingState = JobResultState.Loading(
            latestRefresh = latestRefresh,
          )
          states.emit(loadingState)

          val newJobResults = if (selectedJob == "All") {
            repo.getLatestResults(
              jobNameStartsWith = jobNamePrefix,
              resultFilter = resultFilter,
            )
          } else {
            repo.getResultsForJob(
              selectedJob = selectedJob,
              resultFilter = resultFilter,
            )
          }

          val lr = LocalDateTime.now()

          mutex.withLock {
            latestRefresh = lr
          }

          val loadedState = JobResultState.Loaded(
            jobResults = newJobResults,
            jobNameOptions = jobNameOptions(newJobResults),
            latestRefresh = lr,
          )

          logger.info("Emitting new job results after refresh.")

          states.emit(loadedState)
        }
        is JobResultEvent.FilterChanged -> {
          logger.info(
            "jobNameFilter changed to '${event.jobNamePrefix}', selectedJob changed to '${event.selectedJob}', " +
              "and resultFilter changed to ${event.resultFilter}"
          )

          mutex.withLock {
            selectedJob = event.selectedJob.ifBlank { "All" }
            jobNamePrefix = event.jobNamePrefix
            resultFilter = event.resultFilter
          }

          val newJobResults = if (selectedJob == "All") {
            repo.getLatestResults(
              jobNameStartsWith = jobNamePrefix,
              resultFilter = resultFilter,
            )
          } else {
            repo.getResultsForJob(
              selectedJob = selectedJob,
              resultFilter = resultFilter,
            )
          }

          val newState = JobResultState.Loaded(
            jobResults = newJobResults,
            jobNameOptions = jobNameOptions(newJobResults),
            latestRefresh = latestRefresh ?: error("FilterChanged event triggered, but state was not loaded."),
          )

          states.emit(newState)
        }
      }
      yield()
    }
  }
}

fun jobNameOptions(jobResults: List<JobResult>) =
  listOf("All") + jobResults.map { it.jobName }.sorted()

fun CoroutineScope.refreshJobResultsEvery(
  events: JobResultEvents,
  duration: Duration,
) = launch {
  while (isActive) {
    events.refresh()

    delay(duration)
  }
}
