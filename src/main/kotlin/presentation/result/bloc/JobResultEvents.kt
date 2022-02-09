package presentation.result.bloc

import domain.ResultFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

interface JobResultEvents {
  val stream: SharedFlow<JobResultEvent>

  fun rowSelected(rowNumber: Int)

  fun refresh()

  fun setFilter(selectedJob: String, jobNamePrefix: String, result: ResultFilter)
}

object DefaultJobResultEvents : JobResultEvents {
  private val _stream = MutableSharedFlow<JobResultEvent>()

  override val stream = _stream.asSharedFlow()

  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private val scope: CoroutineScope = MainScope()

  override fun rowSelected(rowNumber: Int) {
    scope.launch {
      withContext(dispatcher) {
        _stream.emit(JobResultEvent.RowSelected(rowNumber))
      }
    }
  }

  override fun refresh() {
    scope.launch {
      withContext(dispatcher) {
        _stream.emit(JobResultEvent.RefreshButtonClicked)
      }
    }
  }

  override fun setFilter(selectedJob: String, jobNamePrefix: String, result: ResultFilter) {
    scope.launch {
      withContext(dispatcher) {
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
}
