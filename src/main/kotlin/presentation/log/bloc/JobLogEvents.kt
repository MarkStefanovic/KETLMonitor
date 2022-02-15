package presentation.log.bloc

import domain.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

interface JobLogEvents {
  val stream: SharedFlow<JobLogEvent>

  fun refresh()

  fun setFilter(jobNamePrefix: String, logLevel: LogLevel)
}

@FlowPreview
class DefaultJobLogEvents : JobLogEvents {
  private val _stream = MutableSharedFlow<JobLogEvent>(
    extraBufferCapacity = 5,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  override val stream: SharedFlow<JobLogEvent> = _stream.asSharedFlow()

  private val scope: CoroutineScope = MainScope()

  override fun refresh() {
    println("${javaClass.simpleName}.refresh()")
    scope.launch {
      _stream.emit(JobLogEvent.Refresh)
    }
  }

  override fun setFilter(jobNamePrefix: String, logLevel: LogLevel) {
    println("${javaClass.simpleName}.setJobNameFilter(jobNamePrefix = $jobNamePrefix, logLevel = $logLevel)")
    scope.launch {
      _stream.emit(JobLogEvent.FilterChanged(prefix = jobNamePrefix, logLevel = logLevel))
    }
  }
}
