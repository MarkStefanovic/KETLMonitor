package presentation.log.bloc

import domain.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.logging.Logger

interface JobLogEvents {
  val stream: SharedFlow<JobLogEvent>

  fun refresh()

  fun setFilter(jobNamePrefix: String, logLevel: LogLevel)
}

class DefaultJobLogEvents(
  private val scope: CoroutineScope,
  private val logger: Logger,
) : JobLogEvents {
  private val _stream = MutableSharedFlow<JobLogEvent>(
    extraBufferCapacity = 5,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  override val stream: SharedFlow<JobLogEvent> = _stream.asSharedFlow()

  override fun refresh() {
    logger.info("${javaClass.simpleName}.refresh()")

    scope.launch {
      _stream.emit(JobLogEvent.Refresh)
    }
  }

  override fun setFilter(jobNamePrefix: String, logLevel: LogLevel) {
    logger.info("${javaClass.simpleName}.setJobNameFilter(jobNamePrefix = $jobNamePrefix, logLevel = $logLevel)")

    scope.launch {
      _stream.emit(JobLogEvent.FilterChanged(prefix = jobNamePrefix, logLevel = logLevel))
    }
  }
}
