package presentation.status.bloc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface JobStatusStates {
  val stream: StateFlow<JobStatusState>

  fun emit(state: JobStatusState)
}

class DefaultJobStatusStates : JobStatusStates {
  private val _stream = MutableStateFlow<JobStatusState>(JobStatusState.Initial)

  override val stream = _stream.asStateFlow()

  override fun emit(state: JobStatusState) {
    _stream.value = state
  }
}
