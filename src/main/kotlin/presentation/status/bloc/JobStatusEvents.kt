package presentation.status.bloc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
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

class DefaultJobStatusEvents : JobStatusEvents {
  private val _stream = MutableSharedFlow<JobStatusEvent>(
    extraBufferCapacity = 3,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  override val stream = _stream.asSharedFlow()

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
        _stream.emit(event)
      }
    }
  }
}
