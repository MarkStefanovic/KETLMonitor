package presentation.log.bloc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

interface JobLogEvents {
  val stream: SharedFlow<JobLogEvent>

  fun refresh()

  fun setJobNameFilter(jobNamePrefix: String)
}

@FlowPreview
object DefaultJobLogEvents : JobLogEvents {
  private val _stream = MutableSharedFlow<JobLogEvent>()

  override val stream: SharedFlow<JobLogEvent> = _stream.asSharedFlow()

  private val scope: CoroutineScope = MainScope()

  override fun refresh() {
    println("${javaClass.simpleName}.refresh()")
    scope.launch {
      _stream.emit(JobLogEvent.Refresh)
    }
  }

  override fun setJobNameFilter(jobNamePrefix: String) {
    println("${javaClass.simpleName}.setJobNameFilter(jobNamePrefix = $jobNamePrefix)")
    scope.launch {
      _stream.emit(JobLogEvent.FilterChanged(jobNamePrefix))
    }
  }
}
