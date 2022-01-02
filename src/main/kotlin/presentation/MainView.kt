package presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.StateFlow
import presentation.log.bloc.JobLogEvents
import presentation.log.bloc.JobLogState
import presentation.log.view.JobLogListView
import presentation.result.bloc.JobResultEvents
import presentation.result.bloc.JobResultState
import presentation.result.view.JobResultListView
import presentation.status.bloc.JobStatusEvents
import presentation.status.bloc.JobStatusState
import presentation.status.view.JobStatusListView

@Composable
@ExperimentalMaterialApi
@ExperimentalFoundationApi
fun MainView(
  jobResultsStateFlow: StateFlow<JobResultState>,
  jobResultsEvents: JobResultEvents,
  jobStatusStateFlow: StateFlow<JobStatusState>,
  jobStatusEvents: JobStatusEvents,
  jobLogStateFlow: StateFlow<JobLogState>,
  jobLogEvents: JobLogEvents,
) {
  var tabIndex by remember { mutableStateOf(0) }

  val tabNames = listOf("Results", "Status", "Log")

  Column {
    TabRow(
      selectedTabIndex = tabIndex,
    ) {
      tabNames.forEachIndexed { index, text ->
        Tab(
          selected = tabIndex == index,
          onClick = {
            tabIndex = index
          },
          text = {
            Text(text = text, fontWeight = FontWeight.ExtraBold)
          },
        )
      }
    }

    when (tabIndex) {
      0 -> {
        JobResultListView(
          stateFlow = jobResultsStateFlow,
          events = jobResultsEvents,
        )
      }
      1 -> {
        JobStatusListView(
          stateFlow = jobStatusStateFlow,
          events = jobStatusEvents,
        )
      }
      2 -> {
        JobLogListView(
          stateFlow = jobLogStateFlow,
          events = jobLogEvents,
        )
      }
    }
  }
}
