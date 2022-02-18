package presentation.status.bloc

import domain.JobStatus
import domain.JobStatusRepo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
fun CoroutineScope.jobStatusBloc(
  states: JobStatusStates,
  events: JobStatusEvents,
  repo: JobStatusRepo,
  logger: Logger,
  dispatcher: CoroutineDispatcher,
) {
  val singleThreadedDispatcher = dispatcher.limitedParallelism(1)

  var latestRefresh: LocalDateTime? = null

  var filter = ""

  val unfilteredJobStatuses = mutableListOf<JobStatus>()

  launch {
    events.stream.collect { event: JobStatusEvent ->
      println("jobStatusBloc received event: $event")

      try {
        withContext(singleThreadedDispatcher) {
          withTimeout(60.seconds) {
            when (event) {
              is JobStatusEvent.Error -> states.emit(
                JobStatusState.Error(
                  latestRefresh = latestRefresh,
                  errorMessage = event.errorMessage,
                )
              )
              is JobStatusEvent.FilterChanged -> {
                filter = event.jobName

                val newState = JobStatusState.Loaded(
                  filter = filter,
                  jobStatuses = filterJobStatuses(
                    jobStatuses = unfilteredJobStatuses,
                    jobNameFilter = filter,
                  ),
                  latestRefresh = latestRefresh ?: error("Latest refresh is null, but the state is Loaded."),
                )

                states.emit(newState)
              }
              JobStatusEvent.RefreshButtonClicked -> {
                states.emit(
                  JobStatusState.Loading(
                    jobStatuses = filterJobStatuses(
                      jobStatuses = unfilteredJobStatuses,
                      jobNameFilter = filter,
                    ),
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
                  jobStatuses = filterJobStatuses(
                    jobStatuses = unfilteredJobStatuses,
                    jobNameFilter = filter,
                  ),
                  latestRefresh = latestRefresh ?: error("Latest refresh is null, but the state is Loaded."),
                )

                states.emit(newState)
              }
            }
          }
        }
      } catch (ce: CancellationException) {
        println("jobStatusBloc closed.")
        throw ce
      } catch (e: Throwable) {
        logger.severe(e.stackTraceToString())

        delay(10.seconds)
      }
    }
  }
}

fun CoroutineScope.autorefreshJobStatuses(
  events: JobStatusEvents,
  duration: Duration,
) {
  launch {
    while (isActive) {
      events.refresh()
      delay(duration)
    }
  }
}

private fun filterJobStatuses(
  jobStatuses: List<JobStatus>,
  jobNameFilter: String,
): List<JobStatus> =
  jobStatuses.filter { it.jobName.lowercase().contains(jobNameFilter.lowercase()) }
