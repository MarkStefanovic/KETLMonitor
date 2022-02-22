package presentation.log.bloc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface JobLogStates {
  val stream: StateFlow<JobLogState>

  fun emit(state: JobLogState)
}

class DefaultJobLogStates : JobLogStates {
  private val _stream = MutableStateFlow<JobLogState>(JobLogState.Initial)

  override val stream = _stream.asStateFlow()

  override fun emit(state: JobLogState) {
    _stream.value = state
  }
}
