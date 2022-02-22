package presentation.result.bloc

import domain.JobResult
import domain.JobResultRepo
import domain.ResultFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
  val singleThreadedContext = newSingleThreadContext("jobResultBloc")

  var latestRefresh: LocalDateTime? = null

  var jobNameFilter = ""

  var resultFilter = ResultFilter.All

  var selectedJob = "All"

  launch {
    events.stream.collect { event ->
      try {
        withContext(singleThreadedContext) {
          println("jobResultBloc received event: $event")

          withTimeout(60.seconds) {
            when (event) {
              is JobResultEvent.RefreshButtonClicked -> {
                val loadingState = JobResultState.Loading(
                  latestRefresh = latestRefresh,
                )
                states.emit(loadingState)

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

                val loadedState = JobResultState.Loaded(
                  jobResults = newJobResults,
                  jobNameOptions = jobNameOptions(newJobResults),
                  latestRefresh = lr,
                )

                println("Emitting new job results after refresh.")

                states.emit(loadedState)
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
                  jobResults = newJobResults,
                  jobNameOptions = jobNameOptions(newJobResults),
                  latestRefresh = latestRefresh ?: error("FilterChanged event triggered, but state was not loaded."),
                )

                states.emit(newState)
              }
            }
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) {
          println("Cancelling jobResultBloc...")
          throw e
        }

        logger.severe(e.stackTraceToString())
        throw e
      }
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
