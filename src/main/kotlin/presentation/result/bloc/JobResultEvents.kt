package presentation.result.bloc

import domain.ResultFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.logging.Logger

interface JobResultEvents {
  val stream: SharedFlow<JobResultEvent>

  fun refresh()

  fun setFilter(
    selectedJob: String,
    jobNamePrefix: String,
    result: ResultFilter,
  )
}

class DefaultJobResultEvents(
  private val scope: CoroutineScope,
  private val logger: Logger,
) : JobResultEvents {
  private val _stream = MutableSharedFlow<JobResultEvent>(
    extraBufferCapacity = 3,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  override val stream = _stream.asSharedFlow()

  override fun refresh() {
    scope.launch {
      _stream.emit(
        JobResultEvent.RefreshButtonClicked
      )
    }
  }

  override fun setFilter(
    selectedJob: String,
    jobNamePrefix: String,
    result: ResultFilter,
  ) {
    scope.launch {
      _stream.emit(
        JobResultEvent.FilterChanged(
          selectedJob = selectedJob,
          jobNamePrefix = jobNamePrefix,
          resultFilter = result,
        )
      )
    }
  }
}
