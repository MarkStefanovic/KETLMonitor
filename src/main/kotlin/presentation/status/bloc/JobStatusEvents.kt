package presentation.status.bloc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.logging.Logger

interface JobStatusEvents {
  val stream: SharedFlow<JobStatusEvent>

  fun refresh()

  fun setFilter(jobName: String)
}

class DefaultJobStatusEvents(
  private val scope: CoroutineScope,
  private val logger: Logger,
) : JobStatusEvents {
  private val _stream = MutableSharedFlow<JobStatusEvent>(
    extraBufferCapacity = 3,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  override val stream = _stream.asSharedFlow()

  override fun refresh() {
    send(JobStatusEvent.RefreshButtonClicked)
  }

  override fun setFilter(jobName: String) {
    send(JobStatusEvent.FilterChanged(jobName))
  }

  private fun send(event: JobStatusEvent) {
    scope.launch {
      _stream.emit(event)
    }
  }
}
