package presentation.status.bloc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

interface JobStatusEvents {
  val stream: SharedFlow<JobStatusEvent>

  fun refresh()

  fun setFilter(jobName: String)
}

object DefaultJobStatusEvents : JobStatusEvents {
  private val backingStream = MutableSharedFlow<JobStatusEvent>()

  override val stream = backingStream.asSharedFlow()

  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private val scope: CoroutineScope = MainScope()

  override fun refresh() {
    send(JobStatusEvent.RefreshButtonClicked)
  }

  override fun setFilter(jobName: String) {
    send(JobStatusEvent.FilterChanged(jobName))
  }

  private fun send(event: JobStatusEvent) {
    scope.launch {
      withContext(dispatcher) {
        backingStream.emit(event)
      }
    }
  }
}
