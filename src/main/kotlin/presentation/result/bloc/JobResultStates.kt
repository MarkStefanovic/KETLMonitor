package presentation.result.bloc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface JobResultStates {
  val stream: StateFlow<JobResultState>

  fun emit(state: JobResultState)
}

class DefaultJobResultStates : JobResultStates {
  private val _stream = MutableStateFlow<JobResultState>(JobResultState.Initial)

  override val stream = _stream.asStateFlow()

  override fun emit(state: JobResultState) {
    _stream.value = state
  }
}
